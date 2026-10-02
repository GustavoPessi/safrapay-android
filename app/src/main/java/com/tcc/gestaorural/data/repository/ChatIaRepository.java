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
import com.tcc.gestaorural.data.local.entity.Anexo;
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
        /** comprovanteGuardado = a foto enviada no chat virou comprovante do lançamento. */
        void onGravado(String tipo, boolean comprovanteGuardado);
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
    private final AnexoRepository anexoRepo;
    private final Handler main = new Handler(Looper.getMainLooper());

    static final String MSG_SESSAO_EXPIRADA =
            "Sua sessão expirou. Entre de novo na sua conta para continuar usando o assistente.";

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
        this.anexoRepo = new AnexoRepository(context);
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
                    if (l.getSafraUuid() == null || l.getValor() == null) continue;
                    Safra s = safraDao.buscarPorUuid(l.getSafraUuid());
                    if (s == null) continue;
                    String data = (l.getData() != null && !l.getData().isEmpty()) ? l.getData() : DateUtils.hojeIso();
                    total += "RECEITA".equalsIgnoreCase(l.getTipo())
                            ? receitaDao.contarIguais(s.id, l.getValor(), data)
                            : despesaDao.contarIguais(s.id, l.getValor(), data);
                }
            } catch (Exception ignored) {
                total = 0; // na dúvida, não bloqueia o lançamento
            }
            final int n = total;
            post(() -> cb.onContagem(n));
        });
    }

    /** Há token e ele ainda não venceu (lido a cada chamada: o usuário pode ter refeito o login). */
    public boolean temToken() {
        // Token vencido não bloqueia mais: se há refresh token, o interceptor renova sozinho.
        String rt = session.getRefreshToken();
        return session.tokenValido() || (rt != null && !rt.isEmpty());
    }

    /**
     * Envia o histórico da conversa + o contexto do usuário pro assistente.
     * O contexto é montado numa thread de IO; a resposta do Retrofit já volta
     * na main thread.
     */
    public void enviar(long usuarioId, List<ChatMensagemRequestDto.Mensagem> historico, RespostaCallback cb) {
        enviar(usuarioId, historico, null, null, cb);
    }

    /** Igual ao enviar normal, com foto OU arquivo anexado à última mensagem do produtor. */
    public void enviar(long usuarioId, List<ChatMensagemRequestDto.Mensagem> historico,
                       ChatMensagemRequestDto.Imagem imagem, ChatMensagemRequestDto.Arquivo arquivo,
                       RespostaCallback cb) {
        enviar(usuarioId, historico, imagem, arquivo, null, cb);
    }

    /** Com foto, arquivo OU mensagem de voz (o servidor transcreve e devolve em "transcricao"). */
    public void enviar(long usuarioId, List<ChatMensagemRequestDto.Mensagem> historico,
                       ChatMensagemRequestDto.Imagem imagem, ChatMensagemRequestDto.Arquivo arquivo,
                       ChatMensagemRequestDto.Audio audio, RespostaCallback cb) {
        if (!temToken()) {
            cb.onErro(session.getToken() == null
                    ? "Você precisa entrar na sua conta pra usar o assistente."
                    : MSG_SESSAO_EXPIRADA);
            return;
        }
        final String token = session.getToken();
        AppDatabase.IO.execute(() -> {
            ChatMensagemRequestDto req = new ChatMensagemRequestDto();
            req.setMensagens(historico);
            req.setHoje(DateUtils.hojeIso());
            req.setImagem(imagem);
            req.setArquivo(arquivo);
            req.setAudio(audio);

            ChatMensagemRequestDto.Contexto ctx = new ChatMensagemRequestDto.Contexto();
            ctx.setCategorias(categoriaDao.listarNomes());

            // Safras (contrato v2: + cultura, talhão, ano, datas em ISO e propriedade)
            List<ChatMensagemRequestDto.Safra> safras = new ArrayList<>();
            for (SafraContexto s : safraDao.listarContexto(usuarioId)) {
                String cultura = (s.getCultura() == null || s.getCultura().trim().isEmpty()) ? "Sem cultura" : s.getCultura();
                String desc = cultura + " • " + s.getAnoSafra();
                if (s.getTalhaoNome() != null && !s.getTalhaoNome().isEmpty()) desc += " • Talhão " + s.getTalhaoNome();
                ChatMensagemRequestDto.Safra dto = new ChatMensagemRequestDto.Safra();
                dto.setUuid(s.getUuid());
                dto.setDescricao(desc);
                dto.setAtiva(s.ativa());
                dto.setCultura(s.getCultura());
                dto.setTalhao(s.getTalhaoNome());
                dto.setAnoSafra(s.getAnoSafra());
                dto.setDataInicio(paraIso(s.getDataInicio()));   // no Room a safra guarda dd/MM/yyyy
                dto.setDataFim(paraIso(s.getDataFim()));
                dto.setPropriedadeUuid(s.getPropriedadeUuid());
                safras.add(dto);
            }
            ctx.setSafras(safras);

            List<ChatMensagemRequestDto.Maquina> maqs = new ArrayList<>();
            for (Maquina m : maquinaDao.listarPorUsuarioSync(usuarioId)) {
                maqs.add(new ChatMensagemRequestDto.Maquina(m.uuid, m.nome));
            }
            ctx.setMaquinas(maqs);

            // Propriedades e talhões (contrato v2) — sem nenhum valor financeiro
            java.util.Map<Long, String> uuidPropriedade = new java.util.HashMap<>();
            List<ChatMensagemRequestDto.PropriedadeCtx> props = new ArrayList<>();
            for (Propriedade p : propriedadeDao.listarPorUsuarioSync(usuarioId)) {
                uuidPropriedade.put(p.id, p.uuid);
                ChatMensagemRequestDto.PropriedadeCtx dto = new ChatMensagemRequestDto.PropriedadeCtx();
                dto.setUuid(p.uuid);
                dto.setNome(p.nome);
                dto.setAreaTotal(p.areaTotal);
                props.add(dto);
            }
            ctx.setPropriedades(props);
            List<ChatMensagemRequestDto.TalhaoCtx> tals = new ArrayList<>();
            for (Talhao t : talhaoDao.listarPorUsuarioSync(usuarioId)) {
                ChatMensagemRequestDto.TalhaoCtx dto = new ChatMensagemRequestDto.TalhaoCtx();
                dto.setUuid(t.uuid);
                dto.setNome(t.nome);
                dto.setPropriedadeUuid(uuidPropriedade.get(t.propriedadeId));
                dto.setCultura(t.cultura);
                dto.setAreaHa(t.areaHa);
                tals.add(dto);
            }
            ctx.setTalhoes(tals);
            req.setContexto(ctx);

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
            case 413: return "O anexo é grande demais (foto até 5 MB, arquivo até 2 MB, áudio até 2 minutos). Tente um menor.";
            case 415: return "Formato não aceito. Foto: JPEG, PNG ou WebP. Arquivo: .xlsx, .csv ou .txt. Áudio: Ogg/Opus ou M4A.";
            case 422: return "Não consegui entender o áudio. Tente gravar de novo, num lugar mais silencioso, ou escreva.";
            case 429: return "Você atingiu o limite de mensagens de hoje. Tente novamente amanhã.";
            case 502: return "O assistente está instável no momento. Tente de novo em instantes.";
            case 503: return "O assistente ainda não está configurado no servidor.";
            case 400: return "Não entendi a mensagem. Tente reescrever de outro jeito.";
            default:  return "Não consegui processar agora (erro " + code + "). Tente de novo.";
        }
    }

    /**
     * Grava um lançamento proposto no Room local (mesmo caminho de um cadastro normal).
     * Se a proposta veio de uma foto (fotoJpeg != null), a foto vira o comprovante do lançamento.
     */
    public void confirmar(ChatMensagemResponseDto.Lancamento l, byte[] fotoJpeg, GravarCallback cb) {
        AppDatabase.IO.execute(() -> {
            try {
                Safra s = (l.getSafraUuid() != null) ? safraDao.buscarPorUuid(l.getSafraUuid()) : null;
                if (s == null) { post(() -> cb.onErro("Safra do lançamento não encontrada.")); return; }

                String agora = DateUtils.agora();
                String data = (l.getData() != null && !l.getData().isEmpty()) ? l.getData() : DateUtils.hojeIso();
                double valor = (l.getValor() != null) ? l.getValor() : 0d;
                String descricao = (l.getDescricao() != null && !l.getDescricao().isEmpty())
                        ? l.getDescricao() : "Lançamento via assistente";

                if ("RECEITA".equalsIgnoreCase(l.getTipo())) {
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
                    boolean foto = guardarComprovante(Anexo.TIPO_RECEITA, r.uuid, fotoJpeg);
                    post(() -> cb.onGravado("RECEITA", foto));
                } else {
                    CategoriaDespesa c = (l.getCategoria() != null) ? categoriaDao.buscarPorNome(l.getCategoria()) : null;
                    if (c == null) { post(() -> cb.onErro("Categoria não reconhecida pra essa despesa.")); return; }
                    Despesa d = new Despesa();
                    d.uuid = UUID.randomUUID().toString();
                    d.safraId = s.id;
                    d.categoriaId = c.id;
                    if (l.getMaquinaUuid() != null && !l.getMaquinaUuid().isEmpty()) {
                        Maquina m = maquinaDao.buscarPorUuid(l.getMaquinaUuid());
                        d.maquinaId = (m != null) ? m.id : null;
                    }
                    d.descricao = descricao;
                    d.valor = valor;
                    d.dataRegistro = data;
                    d.criadoEm = agora;
                    d.atualizadoEm = agora;
                    d.sincronizado = 0;
                    despesaDao.inserir(d);
                    boolean foto = guardarComprovante(Anexo.TIPO_DESPESA, d.uuid, fotoJpeg);
                    post(() -> cb.onGravado("DESPESA", foto));
                }
            } catch (Exception e) {
                post(() -> cb.onErro("Não consegui gravar o lançamento. Tente de novo."));
            }
        });
    }

    /** Falha ao guardar a foto não desfaz o lançamento (ele já foi gravado). */
    private boolean guardarComprovante(String tipo, String lancamentoUuid, byte[] fotoJpeg) {
        if (fotoJpeg == null || fotoJpeg.length == 0) return false;
        try {
            anexoRepo.gravarJpegSync(tipo, lancamentoUuid, fotoJpeg, Anexo.ORIGEM_CHAT_FOTO);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void post(Runnable r) { main.post(r); }
}
