package com.tcc.gestaorural.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.CategoriaDespesaDao;
import com.tcc.gestaorural.data.local.dao.DespesaDao;
import com.tcc.gestaorural.data.local.dao.MaquinaDao;
import com.tcc.gestaorural.data.local.dao.ReceitaDao;
import com.tcc.gestaorural.data.local.dao.SafraDao;
import com.tcc.gestaorural.data.local.entity.CategoriaDespesa;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.data.local.entity.Receita;
import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.data.local.model.SafraContexto;
import com.tcc.gestaorural.data.remote.ApiService;
import com.tcc.gestaorural.data.remote.RetrofitClient;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemRequestDto;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemResponseDto;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Assistente de lançamentos com IA (chat dentro do app). Monta o contexto do
 * usuário a partir do Room, chama o backend (POST /api/chat/mensagem) e, quando
 * o produtor confirma, grava a despesa/receita proposta no Room pelo mesmo
 * caminho de um cadastro normal (uuid + sincronizado=0, sobe no próximo sync).
 */
public class ChatIaRepository {

    public interface RespostaCallback {
        void onResposta(ChatMensagemResponseDto resp);
        void onErro(String mensagem);
    }

    public interface GravarCallback {
        void onGravado(String tipo);
        void onErro(String mensagem);
    }

    private final ApiService api;
    private final SafraDao safraDao;
    private final CategoriaDespesaDao categoriaDao;
    private final MaquinaDao maquinaDao;
    private final DespesaDao despesaDao;
    private final ReceitaDao receitaDao;
    private final String token;
    private final Handler main = new Handler(Looper.getMainLooper());

    public ChatIaRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.api = RetrofitClient.getApi();
        this.safraDao = db.safraDao();
        this.categoriaDao = db.categoriaDespesaDao();
        this.maquinaDao = db.maquinaDao();
        this.despesaDao = db.despesaDao();
        this.receitaDao = db.receitaDao();
        this.token = new SessionManager(context).getToken();
    }

    public boolean temToken() { return token != null && !token.isEmpty(); }

    /**
     * Envia o histórico da conversa + o contexto do usuário pro assistente.
     * O contexto é montado numa thread de IO; a resposta do Retrofit já volta
     * na main thread.
     */
    public void enviar(long usuarioId, List<ChatMensagemRequestDto.Mensagem> historico, RespostaCallback cb) {
        if (!temToken()) {
            cb.onErro("Você precisa entrar na sua conta pra usar o assistente.");
            return;
        }
        AppDatabase.IO.execute(() -> {
            ChatMensagemRequestDto req = new ChatMensagemRequestDto();
            req.mensagens = historico;
            req.hoje = DateUtils.hojeIso();

            ChatMensagemRequestDto.Contexto ctx = new ChatMensagemRequestDto.Contexto();
            ctx.categorias = categoriaDao.listarNomes();

            List<ChatMensagemRequestDto.Safra> safras = new ArrayList<>();
            for (SafraContexto s : safraDao.listarContexto(usuarioId)) {
                String cultura = (s.cultura == null || s.cultura.trim().isEmpty()) ? "Sem cultura" : s.cultura;
                String desc = cultura + " • " + s.anoSafra;
                if (s.talhaoNome != null && !s.talhaoNome.isEmpty()) desc += " • Talhão " + s.talhaoNome;
                boolean ativa = (s.dataFim == null || s.dataFim.trim().isEmpty());
                safras.add(new ChatMensagemRequestDto.Safra(s.uuid, desc, ativa));
            }
            ctx.safras = safras;

            List<ChatMensagemRequestDto.Maquina> maqs = new ArrayList<>();
            for (Maquina m : maquinaDao.listarPorUsuarioSync(usuarioId)) {
                maqs.add(new ChatMensagemRequestDto.Maquina(m.uuid, m.nome));
            }
            ctx.maquinas = maqs;
            req.contexto = ctx;

            api.enviarMensagemChat("Bearer " + token, req).enqueue(new Callback<ChatMensagemResponseDto>() {
                @Override
                public void onResponse(Call<ChatMensagemResponseDto> call, Response<ChatMensagemResponseDto> resp) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        cb.onResposta(resp.body());
                    } else {
                        cb.onErro(mensagemDeErro(resp.code()));
                    }
                }
                @Override
                public void onFailure(Call<ChatMensagemResponseDto> call, Throwable t) {
                    cb.onErro("Não consegui falar com o assistente. Verifique sua internet e tente de novo.");
                }
            });
        });
    }

    private String mensagemDeErro(int code) {
        switch (code) {
            case 429: return "Você atingiu o limite de mensagens de hoje. Tente novamente amanhã.";
            case 502: return "O assistente está instável no momento. Tente de novo em instantes.";
            case 503: return "O assistente ainda não está configurado no servidor.";
            case 400: return "Não entendi a mensagem. Tente reescrever de outro jeito.";
            default:  return "Não consegui processar agora (erro " + code + "). Tente de novo.";
        }
    }

    /** Grava um lançamento proposto no Room local (mesmo caminho de um cadastro normal). */
    public void confirmar(ChatMensagemResponseDto.Lancamento l, GravarCallback cb) {
        AppDatabase.IO.execute(() -> {
            try {
                Safra s = (l.safraUuid != null) ? safraDao.buscarPorUuid(l.safraUuid) : null;
                if (s == null) { post(() -> cb.onErro("Safra do lançamento não encontrada.")); return; }

                String agora = DateUtils.agora();
                String data = (l.data != null && !l.data.isEmpty()) ? l.data : DateUtils.hojeIso();
                double valor = (l.valor != null) ? l.valor : 0d;
                String descricao = (l.descricao != null && !l.descricao.isEmpty())
                        ? l.descricao : "Lançamento via assistente";

                if ("RECEITA".equalsIgnoreCase(l.tipo)) {
                    Receita r = new Receita();
                    r.uuid = UUID.randomUUID().toString();
                    r.safraId = s.id;
                    r.descricao = descricao;
                    r.valor = valor;
                    r.dataRegistro = data;
                    r.criadoEm = agora;
                    r.atualizadoEm = agora;
                    r.sincronizado = 0;
                    receitaDao.inserir(r);
                    post(() -> cb.onGravado("RECEITA"));
                } else {
                    CategoriaDespesa c = (l.categoria != null) ? categoriaDao.buscarPorNome(l.categoria) : null;
                    if (c == null) { post(() -> cb.onErro("Categoria não reconhecida pra essa despesa.")); return; }
                    Despesa d = new Despesa();
                    d.uuid = UUID.randomUUID().toString();
                    d.safraId = s.id;
                    d.categoriaId = c.id;
                    if (l.maquinaUuid != null && !l.maquinaUuid.isEmpty()) {
                        Maquina m = maquinaDao.buscarPorUuid(l.maquinaUuid);
                        d.maquinaId = (m != null) ? m.id : null;
                    }
                    d.descricao = descricao;
                    d.valor = valor;
                    d.dataRegistro = data;
                    d.criadoEm = agora;
                    d.atualizadoEm = agora;
                    d.sincronizado = 0;
                    despesaDao.inserir(d);
                    post(() -> cb.onGravado("DESPESA"));
                }
            } catch (Exception e) {
                post(() -> cb.onErro("Não consegui gravar o lançamento. Tente de novo."));
            }
        });
    }

    private void post(Runnable r) { main.post(r); }
}
