package com.tcc.gestaorural.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.CategoriaDespesaDao;
import com.tcc.gestaorural.data.local.dao.DespesaDao;
import com.tcc.gestaorural.data.local.dao.MaquinaDao;
import com.tcc.gestaorural.data.local.dao.PropriedadeDao;
import com.tcc.gestaorural.data.local.dao.ReceitaDao;
import com.tcc.gestaorural.data.local.dao.SafraDao;
import com.tcc.gestaorural.data.local.dao.TalhaoDao;
import com.tcc.gestaorural.data.local.entity.CategoriaDespesa;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.data.local.entity.Propriedade;
import com.tcc.gestaorural.data.local.entity.Receita;
import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.data.local.entity.Talhao;
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

    public interface ContagemCallback { void onContagem(int quantidade); }

    private final ApiService api;
    private final SafraDao safraDao;
    private final CategoriaDespesaDao categoriaDao;
    private final MaquinaDao maquinaDao;
    private final DespesaDao despesaDao;
    private final ReceitaDao receitaDao;
    private final PropriedadeDao propriedadeDao;
    private final TalhaoDao talhaoDao;
    private final SessionManager session;
    private final Handler main = new Handler(Looper.getMainLooper());

    static final String MSG_SESSAO_EXPIRADA =
            "Sua sessão com o servidor expirou (ela vale 24h). Saia da conta e entre de novo para continuar usando o assistente.";

    public ChatIaRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.api = RetrofitClient.getApi();
        this.safraDao = db.safraDao();
        this.categoriaDao = db.categoriaDespesaDao();
        this.maquinaDao = db.maquinaDao();
        this.despesaDao = db.despesaDao();
        this.receitaDao = db.receitaDao();
        this.propriedadeDao = db.propriedadeDao();
        this.talhaoDao = db.talhaoDao();
        this.session = new SessionManager(context);
    }

    /** Datas da safra ficam em dd/MM/yyyy no Room; o contrato manda yyyy-MM-dd. */
    private static String paraIso(String data) {
        if (data == null || data.trim().isEmpty()) return null;
        String d = data.trim();
        if (d.matches("\\d{4}-\\d{2}-\\d{2}")) return d;
        return DateUtils.brParaIso(d);
    }

    /**
     * Conta possíveis duplicatas (mesma safra, mesmo valor e mesma data) dos
     * lançamentos propostos, pra o app perguntar antes de gravar de novo.
     */
    public void contarDuplicatas(List<ChatMensagemResponseDto.Lancamento> lancamentos, ContagemCallback cb) {
        AppDatabase.IO.execute(() -> {
            int total = 0;
            try {
                for (ChatMensagemResponseDto.Lancamento l : lancamentos) {
                    if (l.safraUuid == null || l.valor == null) continue;
                    Safra s = safraDao.buscarPorUuid(l.safraUuid);
                    if (s == null) continue;
                    String data = (l.data != null && !l.data.isEmpty()) ? l.data : DateUtils.hojeIso();
                    total += "RECEITA".equalsIgnoreCase(l.tipo)
                            ? receitaDao.contarIguais(s.id, l.valor, data)
                            : despesaDao.contarIguais(s.id, l.valor, data);
                }
            } catch (Exception ignored) {
                total = 0; // na dúvida, não bloqueia o lançamento
            }
            final int n = total;
            post(() -> cb.onContagem(n));
        });
    }

    /** Há token e ele ainda não venceu (lido a cada chamada: o usuário pode ter refeito o login). */
    public boolean temToken() { return session.tokenValido(); }

    /**
     * Envia o histórico da conversa + o contexto do usuário pro assistente.
     * O contexto é montado numa thread de IO; a resposta do Retrofit já volta
     * na main thread.
     */
    public void enviar(long usuarioId, List<ChatMensagemRequestDto.Mensagem> historico, RespostaCallback cb) {
        if (!temToken()) {
            cb.onErro(session.getToken() == null
                    ? "Você precisa entrar na sua conta pra usar o assistente."
                    : MSG_SESSAO_EXPIRADA);
            return;
        }
        final String token = session.getToken();
        AppDatabase.IO.execute(() -> {
            ChatMensagemRequestDto req = new ChatMensagemRequestDto();
            req.mensagens = historico;
            req.hoje = DateUtils.hojeIso();

            ChatMensagemRequestDto.Contexto ctx = new ChatMensagemRequestDto.Contexto();
            ctx.categorias = categoriaDao.listarNomes();

            // Safras (contrato v2: + cultura, talhão, ano, datas em ISO e propriedade)
            List<ChatMensagemRequestDto.Safra> safras = new ArrayList<>();
            for (SafraContexto s : safraDao.listarContexto(usuarioId)) {
                String cultura = (s.cultura == null || s.cultura.trim().isEmpty()) ? "Sem cultura" : s.cultura;
                String desc = cultura + " • " + s.anoSafra;
                if (s.talhaoNome != null && !s.talhaoNome.isEmpty()) desc += " • Talhão " + s.talhaoNome;
                ChatMensagemRequestDto.Safra dto = new ChatMensagemRequestDto.Safra();
                dto.uuid = s.uuid;
                dto.descricao = desc;
                dto.ativa = s.ativa();
                dto.cultura = s.cultura;
                dto.talhao = s.talhaoNome;
                dto.anoSafra = s.anoSafra;
                dto.dataInicio = paraIso(s.dataInicio);   // no Room a safra guarda dd/MM/yyyy
                dto.dataFim = paraIso(s.dataFim);
                dto.propriedadeUuid = s.propriedadeUuid;
                safras.add(dto);
            }
            ctx.safras = safras;

            List<ChatMensagemRequestDto.Maquina> maqs = new ArrayList<>();
            for (Maquina m : maquinaDao.listarPorUsuarioSync(usuarioId)) {
                maqs.add(new ChatMensagemRequestDto.Maquina(m.uuid, m.nome));
            }
            ctx.maquinas = maqs;

            // Propriedades e talhões (contrato v2) — sem nenhum valor financeiro
            java.util.Map<Long, String> uuidPropriedade = new java.util.HashMap<>();
            List<ChatMensagemRequestDto.PropriedadeCtx> props = new ArrayList<>();
            for (Propriedade p : propriedadeDao.listarPorUsuarioSync(usuarioId)) {
                uuidPropriedade.put(p.id, p.uuid);
                ChatMensagemRequestDto.PropriedadeCtx dto = new ChatMensagemRequestDto.PropriedadeCtx();
                dto.uuid = p.uuid;
                dto.nome = p.nome;
                dto.areaTotal = p.areaTotal;
                props.add(dto);
            }
            ctx.propriedades = props;
            List<ChatMensagemRequestDto.TalhaoCtx> tals = new ArrayList<>();
            for (Talhao t : talhaoDao.listarPorUsuarioSync(usuarioId)) {
                ChatMensagemRequestDto.TalhaoCtx dto = new ChatMensagemRequestDto.TalhaoCtx();
                dto.uuid = t.uuid;
                dto.nome = t.nome;
                dto.propriedadeUuid = uuidPropriedade.get(t.propriedadeId);
                dto.cultura = t.cultura;
                dto.areaHa = t.areaHa;
                tals.add(dto);
            }
            ctx.talhoes = tals;
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
            case 401:
            case 403: return MSG_SESSAO_EXPIRADA;
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
