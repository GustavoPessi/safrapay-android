package com.tcc.gestaorural.data.sync;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.util.SessionManager;

import java.util.Set;

/**
 * Sincronização automática (decidida em 07/10/2026), além do botão "Sincronizar dados":
 * 1. ao abrir o app (ou voltar do segundo plano) — antes confere o papel na equipe, pra não
 *    mandar dados pra fazenda errada se a pessoa entrou/saiu de uma equipe em outro aparelho;
 * 2. ao reconectar a internet;
 * 3. depois de qualquer alteração local, esperando {@link #ESPERA_ALTERACAO_MS} sem nova
 *    alteração (agrupa vários salvamentos numa sincronização só).
 * Tudo em silêncio (erro não mostra nada: o próximo gatilho tenta de novo) e com a trava do
 * {@link SyncManager} — nunca duas sincronizações ao mesmo tempo.
 *
 * Não é tempo real: um aparelho parado, sempre conectado e sem alterações próprias só vê o
 * que o outro mandou no próximo gatilho (aviso do servidor exigiria notificação push).
 */
public final class SincronizacaoAutomatica {

    private static final long ESPERA_ALTERACAO_MS = 4_000;
    private static final long ESPERA_REDE_MS = 2_000;

    private static SincronizacaoAutomatica instancia;

    private final Context ctx;
    private final SessionManager session;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int telasVisiveis = 0;
    /** O app acabou de abrir/voltar: conferir a equipe assim que uma tela do app ficar visível. */
    private boolean conferirEquipeNaProximaTela = false;
    private boolean pedidoDuranteSync = false;

    private final Runnable sincronizarSeTiverAlteracao = this::sincronizarSeTiverAlteracao;
    /** Só com o app aberto (em segundo plano não gasta dados). */
    private final Runnable sincronizarAposRede = () -> { if (telasVisiveis > 0) disparar(true); };

    private SincronizacaoAutomatica(Application app) {
        this.ctx = app.getApplicationContext();
        this.session = new SessionManager(ctx);
    }

    /** Chamado uma vez no GestaoRuralApp.onCreate. */
    public static synchronized void iniciar(Application app) {
        if (instancia != null) return;
        instancia = new SincronizacaoAutomatica(app);
        instancia.observarAberturaDoApp(app);
        instancia.observarRede();
        instancia.observarAlteracoes();
    }

    // --- Gatilho 1: abrir o app / voltar do segundo plano ---------------

    private void observarAberturaDoApp(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(@NonNull Activity a) {
                if (telasVisiveis++ == 0) aoAbrir();
            }
            @Override public void onActivityStopped(@NonNull Activity a) {
                if (telasVisiveis > 0) telasVisiveis--;
            }
            @Override public void onActivityCreated(@NonNull Activity a, Bundle b) { }
            @Override public void onActivityResumed(@NonNull Activity a) {
                // Splash/login não contam (não são BaseActivity): espera a primeira tela de verdade,
                // pra o aviso de troca de conta aparecer e a navegação ir pra tela certa.
                if (conferirEquipeNaProximaTela && a instanceof com.tcc.gestaorural.ui.BaseActivity) {
                    conferirEquipeNaProximaTela = false;
                    conferirEquipeESincronizar(a);
                }
            }
            @Override public void onActivityPaused(@NonNull Activity a) { }
            @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) { }
            @Override public void onActivityDestroyed(@NonNull Activity a) { }
        });
    }

    private void aoAbrir() {
        if (logado()) conferirEquipeNaProximaTela = true;
    }

    /**
     * Primeiro o papel (com tela: mostra "Você agora está na fazenda de…" e leva pra tela certa);
     * se não trocou de conta, sincroniza. Se trocou, a própria troca já apaga e baixa tudo.
     */
    private void conferirEquipeESincronizar(Activity a) {
        com.tcc.gestaorural.ui.equipe.EquipeSincronizador.verificar(a, s -> {
            if (s.getMensagemTroca() == null) disparar(true);
        });
    }

    // --- Gatilho 2: a internet voltou --------------------------------------

    private void observarRede() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        try {
            cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(@NonNull Network network) {
                    // Espera a conexão firmar (o Android avisa várias vezes seguidas ao trocar de rede).
                    main.removeCallbacks(sincronizarAposRede);
                    main.postDelayed(sincronizarAposRede, ESPERA_REDE_MS);
                }
            });
        } catch (Exception ignored) {
            // Sem permissão/serviço de rede: fica só com os outros gatilhos e o botão.
        }
    }

    // --- Gatilho 3: alteração local (com espera) ---------------------------

    private void observarAlteracoes() {
        AppDatabase.getInstance(ctx).getInvalidationTracker().addObserver(
                new InvalidationTracker.Observer(AppDatabase.TABELAS_SINCRONIZADAS) {
                    @Override public void onInvalidated(@NonNull Set<String> tabelas) {
                        // Cada nova alteração reinicia a espera.
                        main.removeCallbacks(sincronizarSeTiverAlteracao);
                        main.postDelayed(sincronizarSeTiverAlteracao, ESPERA_ALTERACAO_MS);
                    }
                });
    }

    /** As gravações da própria sincronização também avisam aqui; só sincroniza se sobrou algo pendente. */
    private void sincronizarSeTiverAlteracao() {
        if (!podeSincronizar()) return;
        AppDatabase.IO.execute(() -> {
            boolean pendente;
            try { pendente = AppDatabase.getInstance(ctx).temAlteracoesPendentes(); }
            catch (Exception e) { pendente = false; }
            if (pendente) main.post(() -> disparar(false));
        });
    }

    // ------------------------------------------------------------------------

    /**
     * @param pull true = sincroniza mesmo sem alteração local (pra receber o que mudou no servidor).
     */
    private void disparar(boolean pull) {
        if (!podeSincronizar() || !temInternet()) return;
        if (SyncManager.emAndamento()) {
            // Já tem uma rodando: faz mais uma passada no fim, pra não perder a alteração nova.
            pedidoDuranteSync = true;
            return;
        }
        new SyncManager(ctx).sincronizarTudo(new SyncManager.Callback() {
            @Override public void onSucesso() { depois(); }
            @Override public void onErro(String mensagem) { pedidoDuranteSync = false; /* silencioso */ }
        });
    }

    private void depois() {
        if (!pedidoDuranteSync) return;
        pedidoDuranteSync = false;
        sincronizarSeTiverAlteracao();
    }

    private boolean logado() {
        return session.estaLogado() && session.getToken() != null && !session.sessaoEncerrada();
    }

    /**
     * Logado, não é colaborador (o app dele é online, sem sync) e o papel não está pendente de
     * conferência (depois de um 403 PAPEL / 409 FAZENDA, só volta a sincronizar quando a tela
     * conferir /api/equipe/minha — senão seriam chamadas inúteis recusadas pelo servidor).
     */
    private boolean podeSincronizar() {
        return logado() && !session.ehColaborador() && !session.papelDesatualizado();
    }

    private boolean temInternet() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        try {
            Network n = cm.getActiveNetwork();
            NetworkCapabilities c = n == null ? null : cm.getNetworkCapabilities(n);
            return c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return true; // na dúvida, tenta (o sync falha em silêncio se não tiver rede)
        }
    }
}
