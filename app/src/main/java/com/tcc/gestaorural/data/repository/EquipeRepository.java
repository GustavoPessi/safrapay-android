package com.tcc.gestaorural.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.remote.ApiService;
import com.tcc.gestaorural.data.remote.ErroApi;
import com.tcc.gestaorural.data.remote.RetrofitClient;
import com.tcc.gestaorural.data.remote.dto.ConviteRecebidoDto;
import com.tcc.gestaorural.data.remote.dto.ConviteRequestDto;
import com.tcc.gestaorural.data.remote.dto.EquipeMinhaResponseDto;
import com.tcc.gestaorural.data.remote.dto.MembroEquipeDto;
import com.tcc.gestaorural.data.remote.dto.PapelRequestDto;
import com.tcc.gestaorural.data.sync.SyncManager;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Multiusuário (contrato de 06/10/2026): papel de quem está logado, convites e gestão da
 * equipe. Também cuida da TROCA DE CONTA: quando a pessoa entra numa equipe, sai ou é
 * removida, os dados da fazenda no aparelho são apagados e baixados de novo da conta certa.
 */
public class EquipeRepository {

    /** Resultado de atualizar o papel. mensagemTroca != null = trocou de conta (avisar o usuário). */
    public static class Situacao {
        private final String papel;
        private final String mensagemTroca;
        private final int convitesPendentes;

        Situacao(String papel, String mensagemTroca, int convitesPendentes) {
            this.papel = papel;
            this.mensagemTroca = mensagemTroca;
            this.convitesPendentes = convitesPendentes;
        }

        public String getPapel() { return papel; }
        public String getMensagemTroca() { return mensagemTroca; }
        public int getConvitesPendentes() { return convitesPendentes; }
    }

    public interface SituacaoCallback {
        void onSituacao(Situacao s);
        void onErro(String mensagem);
    }

    public interface ListaCallback<T> {
        void onLista(List<T> itens);
        void onErro(String mensagem);
    }

    public interface Callback {
        void onOk();
        void onErro(String mensagem);
    }

    private static final String SEM_INTERNET = "Sem conexão com o servidor. Verifique sua internet e tente de novo.";

    private final Context ctx;
    private final ApiService api;
    private final SessionManager session;
    private final Handler main = new Handler(Looper.getMainLooper());

    public EquipeRepository(Context context) {
        this.ctx = context.getApplicationContext();
        this.api = RetrofitClient.getApi();
        this.session = new SessionManager(ctx);
    }

    private String bearer() { return "Bearer " + session.getToken(); }

    // --- Papel / troca de conta -----------------------------------------

    /** GET /api/equipe/minha: guarda o papel e, se mudou de fazenda, troca os dados do aparelho. */
    public void atualizarPapel(SituacaoCallback cb) {
        if (session.getToken() == null) { cb.onErro(SEM_INTERNET); return; }
        api.minhaEquipe(bearer()).enqueue(new retrofit2.Callback<EquipeMinhaResponseDto>() {
            @Override public void onResponse(Call<EquipeMinhaResponseDto> c, Response<EquipeMinhaResponseDto> r) {
                if (r.isSuccessful() && r.body() != null) aplicar(r.body(), cb);
                else cb.onErro(ErroApi.mensagem(r, "Não consegui carregar sua equipe agora."));
            }
            @Override public void onFailure(Call<EquipeMinhaResponseDto> c, Throwable t) { cb.onErro(SEM_INTERNET); }
        });
    }

    /**
     * Aceitar convite: ANTES sobe o que estiver pendente da própria conta (depois de aceitar,
     * o servidor gravaria esses dados na fazenda do dono). Se não conseguir sincronizar, não aceita.
     */
    public void aceitarConvite(String conviteUuid, SituacaoCallback cb) {
        sincronizarAntes(session.ehColaborador() ? null : "Não consegui enviar seus dados antes de entrar na equipe. "
                + "Verifique a internet e tente de novo (assim nada se perde).", () ->
                api.aceitarConvite(bearer(), conviteUuid).enqueue(respostaMinha(cb,
                        "Não consegui aceitar o convite.")), cb);
    }

    public void recusarConvite(String conviteUuid, Callback cb) {
        api.recusarConvite(bearer(), conviteUuid).enqueue(respostaVazia(cb, "Não consegui recusar o convite."));
    }

    /** Sair da equipe: o gerente sobe antes o que fez offline (vai pra fazenda do dono). */
    public void sairDaEquipe(SituacaoCallback cb) {
        sincronizarAntes(session.ehGerente() ? "Não consegui enviar o que você lançou antes de sair. "
                + "Verifique a internet e tente de novo (assim nada se perde)." : null, () ->
                api.sairDaEquipe(bearer()).enqueue(respostaMinha(cb, "Não consegui sair da equipe.")), cb);
    }

    /** mensagemSeFalhar == null: não precisa sincronizar (colaborador não tem dados offline). */
    private void sincronizarAntes(String mensagemSeFalhar, Runnable depois, SituacaoCallback cb) {
        if (mensagemSeFalhar == null) { depois.run(); return; }
        new SyncManager(ctx).sincronizarTudo(new SyncManager.Callback() {
            @Override public void onSucesso() { depois.run(); }
            @Override public void onErro(String mensagem) { cb.onErro(mensagemSeFalhar); }
        });
    }

    private retrofit2.Callback<EquipeMinhaResponseDto> respostaMinha(SituacaoCallback cb, String padrao) {
        return new retrofit2.Callback<EquipeMinhaResponseDto>() {
            @Override public void onResponse(Call<EquipeMinhaResponseDto> c, Response<EquipeMinhaResponseDto> r) {
                if (r.isSuccessful() && r.body() != null) aplicar(r.body(), cb);
                else cb.onErro(ErroApi.mensagem(r, padrao));
            }
            @Override public void onFailure(Call<EquipeMinhaResponseDto> c, Throwable t) { cb.onErro(SEM_INTERNET); }
        };
    }

    /**
     * Guarda papel/dono/pessoas e decide se os dados do aparelho são da fazenda certa:
     * DONO → os próprios; GERENTE/COLABORADOR → os do dono. Se mudou: apaga os dados locais
     * e baixa tudo da conta nova (o colaborador não baixa nada: o app dele é online).
     */
    private void aplicar(EquipeMinhaResponseDto dto, SituacaoCallback cb) {
        String papel = dto.getPapel() == null ? SessionManager.PAPEL_DONO : dto.getPapel().trim().toUpperCase();
        String donoUuid = dto.getDono() != null ? dto.getDono().getUuid() : null;
        String donoNome = dto.getDono() != null ? dto.getDono().getNome() : null;
        int convites = dto.getConvitesPendentes() != null ? dto.getConvitesPendentes() : 0;
        String donoAnteriorNome = session.getDonoNome();
        String papelAnterior = session.getPapel();

        session.salvarEquipe(papel, donoUuid, donoNome,
                new Gson().toJson(dto.getPessoas() != null ? dto.getPessoas() : new ArrayList<>()), convites);

        boolean dono = SessionManager.PAPEL_DONO.equals(papel);
        boolean colaborador = SessionManager.PAPEL_COLABORADOR.equals(papel);
        // O colaborador não baixa os dados da fazenda (app dele é online), então a "fazenda" dele
        // fica marcada diferente da do gerente: virar gerente na mesma fazenda tem que baixar tudo
        // (antes a troca colaborador → gerente não baixava nada — bug do teste de 08/10/2026).
        String fazendaNova = dono || donoUuid == null ? SessionManager.FAZENDA_PROPRIA
                : colaborador ? SessionManager.FAZENDA_COLABORADOR + donoUuid : donoUuid;
        String fazendaAtual = session.getFazendaCarregada();

        if (fazendaAtual == null && dono) {
            // Primeira vez depois da atualização do app: o que está no aparelho já é da própria conta.
            session.setFazendaCarregada(SessionManager.FAZENDA_PROPRIA);
            main.post(() -> cb.onSituacao(new Situacao(papel, null, convites)));
            return;
        }
        if (fazendaNova.equals(fazendaAtual) && !session.pullCompletoPendente()) {
            if (!dono && !colaborador) corrigirGerenteSemDados();
            main.post(() -> cb.onSituacao(new Situacao(papel, null, convites)));
            return;
        }

        String nomeDono = donoNome != null ? donoNome : "outro produtor";
        boolean mesmaFazenda = !dono && donoUuid != null && fazendaAtual != null && fazendaAtual.endsWith(donoUuid);
        String aviso;
        if (fazendaNova.equals(fazendaAtual)) aviso = null; // só terminando um pull que tinha falhado
        else if (mesmaFazenda && !papel.equals(papelAnterior))
            aviso = "Seu papel na fazenda de " + nomeDono + " mudou: agora você é " + (colaborador ? "colaborador" : "gerente") + ".";
        else if (!dono) aviso = "Você agora está na fazenda de " + nomeDono + ".";
        else aviso = "Você saiu da equipe" + (donoAnteriorNome != null ? " de " + donoAnteriorNome : "")
                + "; seus dados voltaram.";

        final boolean baixar = !colaborador;
        AppDatabase.IO.execute(() -> {
            // Com a trava de sincronização: não apaga no meio de uma sincronização automática.
            SyncManager.comTrava(() -> {
                if (!fazendaNova.equals(session.getFazendaCarregada())) {
                    AppDatabase.getInstance(ctx).limparDadosDaFazenda(session.getUsuarioId());
                    new SyncManager(ctx).esquecerMarcadores();
                    session.setFazendaCarregada(fazendaNova);
                }
                session.setPullCompletoPendente(baixar);
            });
            main.post(() -> {
                cb.onSituacao(new Situacao(papel, aviso, convites));
                if (baixar) baixarTudo();
            });
        });
    }

    /**
     * Conserto de aparelhos que ficaram no estado do bug de 08/10 (eram colaborador, viraram
     * gerente na mesma fazenda e nada foi baixado): gerente sem nenhuma propriedade nem máquina
     * no aparelho → esquece os marcadores e baixa tudo. Se a fazenda estiver mesmo vazia, o pull
     * só volta vazio (sem custo).
     */
    private void corrigirGerenteSemDados() {
        AppDatabase.IO.execute(() -> {
            boolean vazio;
            try (android.database.Cursor c = AppDatabase.getInstance(ctx).getOpenHelper().getReadableDatabase().query(
                    "SELECT (SELECT COUNT(*) FROM propriedade WHERE usuario_id = ?) + (SELECT COUNT(*) FROM maquina WHERE usuario_id = ?)",
                    new Object[]{ session.getUsuarioId(), session.getUsuarioId() })) {
                vazio = c.moveToFirst() && c.getLong(0) == 0;
            } catch (Exception e) {
                vazio = false;
            }
            if (!vazio) return;
            new SyncManager(ctx).esquecerMarcadores();
            session.setPullCompletoPendente(true);
            main.post(this::baixarTudo);
        });
    }

    /** Pull completo da conta nova; se falhar, tenta de novo na próxima vez que o papel for conferido. */
    private void baixarTudo() {
        new SyncManager(ctx).sincronizarTudo(new SyncManager.Callback() {
            @Override public void onSucesso() { session.setPullCompletoPendente(false); }
            @Override public void onErro(String mensagem) { /* fica pendente */ }
        });
    }

    // --- Convites recebidos ------------------------------------------------

    public void convitesRecebidos(ListaCallback<ConviteRecebidoDto> cb) {
        api.convitesRecebidos(bearer()).enqueue(respostaLista(cb, "Não consegui carregar os convites."));
    }

    // --- Gestão da equipe (só o dono) --------------------------------------

    public void listarEquipe(ListaCallback<MembroEquipeDto> cb) {
        api.listarEquipe(bearer()).enqueue(respostaLista(cb, "Não consegui carregar sua equipe."));
    }

    public void convidar(String email, String papel, Callback cb) {
        api.convidar(bearer(), new ConviteRequestDto(email, papel)).enqueue(new retrofit2.Callback<MembroEquipeDto>() {
            @Override public void onResponse(Call<MembroEquipeDto> c, Response<MembroEquipeDto> r) {
                if (r.isSuccessful()) { cb.onOk(); return; }
                String padrao = r.code() == 409 ? "Já existe um convite ativo para esse e-mail."
                        : r.code() == 400 ? "Confira o e-mail (não dá pra convidar você mesmo)." : "Não consegui enviar o convite.";
                cb.onErro(ErroApi.mensagem(r, padrao));
            }
            @Override public void onFailure(Call<MembroEquipeDto> c, Throwable t) { cb.onErro(SEM_INTERNET); }
        });
    }

    public void trocarPapel(String membroUuid, String papel, Callback cb) {
        api.trocarPapel(bearer(), membroUuid, new PapelRequestDto(papel)).enqueue(new retrofit2.Callback<MembroEquipeDto>() {
            @Override public void onResponse(Call<MembroEquipeDto> c, Response<MembroEquipeDto> r) {
                if (r.isSuccessful()) cb.onOk(); else cb.onErro(ErroApi.mensagem(r, "Não consegui trocar o papel."));
            }
            @Override public void onFailure(Call<MembroEquipeDto> c, Throwable t) { cb.onErro(SEM_INTERNET); }
        });
    }

    public void remover(String membroUuid, Callback cb) {
        api.removerMembro(bearer(), membroUuid).enqueue(respostaVazia(cb, "Não consegui remover."));
    }

    // ------------------------------------------------------------------------

    private <T> retrofit2.Callback<List<T>> respostaLista(ListaCallback<T> cb, String padrao) {
        return new retrofit2.Callback<List<T>>() {
            @Override public void onResponse(Call<List<T>> c, Response<List<T>> r) {
                if (r.isSuccessful()) cb.onLista(r.body() != null ? r.body() : new ArrayList<>());
                else cb.onErro(ErroApi.mensagem(r, padrao));
            }
            @Override public void onFailure(Call<List<T>> c, Throwable t) { cb.onErro(SEM_INTERNET); }
        };
    }

    private retrofit2.Callback<Void> respostaVazia(Callback cb, String padrao) {
        return new retrofit2.Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) cb.onOk(); else cb.onErro(ErroApi.mensagem(r, padrao));
            }
            @Override public void onFailure(Call<Void> c, Throwable t) { cb.onErro(SEM_INTERNET); }
        };
    }

    /** Lista "pessoas" guardada na sessão (pra escolher responsável de tarefa, até offline). */
    public static List<EquipeMinhaResponseDto.Pessoa> pessoas(Context context) {
        String json = new SessionManager(context).getPessoasJson();
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            EquipeMinhaResponseDto.Pessoa[] arr = new Gson().fromJson(json, EquipeMinhaResponseDto.Pessoa[].class);
            List<EquipeMinhaResponseDto.Pessoa> out = new ArrayList<>();
            if (arr != null) for (EquipeMinhaResponseDto.Pessoa p : arr) if (p != null && p.getUsuarioUuid() != null) out.add(p);
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
