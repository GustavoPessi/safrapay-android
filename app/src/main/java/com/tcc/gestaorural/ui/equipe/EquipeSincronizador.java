package com.tcc.gestaorural.ui.equipe;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.tcc.gestaorural.data.repository.EquipeRepository;
import com.tcc.gestaorural.ui.colaborador.ColaboradorActivity;
import com.tcc.gestaorural.ui.dashboard.DashboardActivity;
import com.tcc.gestaorural.util.SessionManager;

/**
 * Confere o papel no servidor (GET /api/equipe/minha) e ajusta o app:
 * - avisa quando trocou de conta ("Você agora está na fazenda de Fulano");
 * - leva o colaborador pra tela dele e o dono/gerente pro Início, se estiverem no lugar errado.
 * Roda ao abrir/voltar pro app (pela SincronizacaoAutomatica, na primeira tela visível), depois
 * do login e quando o servidor recusa algo por papel (403 PAPEL) ou fazenda (409 FAZENDA).
 */
public final class EquipeSincronizador {

    public interface Ouvinte { void onAtualizado(EquipeRepository.Situacao s); }

    /** Telas que mostram algo da equipe (Início, tela do colaborador) e se redesenham após a conferência. */
    public interface AtualizaComEquipe { void aoAtualizarEquipe(); }

    private EquipeSincronizador() {}

    /** Telas do app do colaborador (dono/gerente não usam). */
    public interface TelaDoColaborador { }

    /** Telas que servem pra qualquer papel (perfil, convites, configurações, sobre). */
    public interface TelaComum { }

    public static void verificar(Activity a, Ouvinte ouvinte) {
        SessionManager s = new SessionManager(a);
        if (!s.estaLogado()) return;
        new EquipeRepository(a).atualizarPapel(new EquipeRepository.SituacaoCallback() {
            @Override public void onSituacao(EquipeRepository.Situacao sit) {
                s.limparPapelDesatualizado(); // só depois de conferir de verdade (offline continua pendente)
                if (a.isFinishing()) return;
                if (sit.getMensagemTroca() != null) Toast.makeText(a, sit.getMensagemTroca(), Toast.LENGTH_LONG).show();
                if (levarParaTelaCerta(a)) return;
                if (a instanceof AtualizaComEquipe) ((AtualizaComEquipe) a).aoAtualizarEquipe();
                if (ouvinte != null) ouvinte.onAtualizado(sit);
            }
            @Override public void onErro(String mensagem) { /* sem internet: confere de novo na próxima abertura */ }
        });
    }

    /** @return true se abriu outra tela (a atual foi fechada). */
    public static boolean levarParaTelaCerta(Activity a) {
        boolean colaborador = new SessionManager(a).ehColaborador();
        boolean telaDoColaborador = a instanceof TelaDoColaborador;
        boolean certa = (a instanceof TelaComum) || (colaborador == telaDoColaborador);
        if (certa) return false;
        Intent i = new Intent(a, colaborador ? ColaboradorActivity.class : DashboardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
        a.finish();
        return true;
    }

    /** Tela inicial certa pro papel guardado (usado no splash e no login). */
    public static Class<?> telaInicial(SessionManager s) {
        return s.ehColaborador() ? ColaboradorActivity.class : DashboardActivity.class;
    }
}
