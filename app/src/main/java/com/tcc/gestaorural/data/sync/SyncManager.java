package com.tcc.gestaorural.data.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.AnotacaoMaquinaDao;
import com.tcc.gestaorural.data.local.dao.CategoriaDespesaDao;
import com.tcc.gestaorural.data.local.dao.DespesaDao;
import com.tcc.gestaorural.data.local.dao.MaquinaDao;
import com.tcc.gestaorural.data.local.dao.MetaFinanceiraDao;
import com.tcc.gestaorural.data.local.dao.PropriedadeDao;
import com.tcc.gestaorural.data.local.dao.ReceitaDao;
import com.tcc.gestaorural.data.local.dao.SafraDao;
import com.tcc.gestaorural.data.local.dao.TalhaoDao;
import com.tcc.gestaorural.data.local.entity.AnotacaoMaquina;
import com.tcc.gestaorural.data.local.entity.CategoriaDespesa;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.data.local.entity.MetaFinanceira;
import com.tcc.gestaorural.data.local.entity.Propriedade;
import com.tcc.gestaorural.data.local.entity.Receita;
import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.data.local.entity.Talhao;
import com.tcc.gestaorural.data.remote.ApiService;
import com.tcc.gestaorural.data.remote.RetrofitClient;
import com.tcc.gestaorural.data.remote.dto.AnotacaoMaquinaSyncItemDto;
import com.tcc.gestaorural.data.remote.dto.DespesaSyncItemDto;
import com.tcc.gestaorural.data.remote.dto.MaquinaSyncItemDto;
import com.tcc.gestaorural.data.remote.dto.MetaFinanceiraSyncItemDto;
import com.tcc.gestaorural.data.remote.dto.PropriedadeSyncItemDto;
import com.tcc.gestaorural.data.remote.dto.ReceitaSyncItemDto;
import com.tcc.gestaorural.data.remote.dto.SafraSyncItemDto;
import com.tcc.gestaorural.data.remote.dto.TalhaoSyncItemDto;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.SessionManager;
import com.tcc.gestaorural.util.SettingsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Response;

/**
 * Motor de sincronização offline-first entre o Room local e o backend SafraPay.
 *
 * Disparado manualmente (botão "Sincronizar" no Perfil - decisão de 11/09/2026:
 * nada de sincronização automática em background por enquanto). Cobre a Fase 1
 * (Propriedade + Talhão), a Fase 2 (Safra), a Fase 3 (Receita + Despesa +
 * Meta Financeira) e a Fase 4 (Máquina + Anotação de máquina) - Fases 2, 3 e 4
 * adicionadas em 28/09/2026.
 *
 * OBS (28/09/2026): a entidade Cultura NÃO faz parte da sincronização - o app
 * abandonou esse modelo em 14/09/2026 (a cultura vem do talhão, campo texto
 * livre em Talhão.cultura, já sincronizado desde a Fase 1). O que existe hoje
 * em data/local/entity/Cultura.java e ui/cultura/ é código órfão, sem tela que
 * navegue até ele - mantido por enquanto, não removido nesta tarefa.
 *
 * Regra de ordem: entidades-pai sincronizam sempre antes das filhas (aqui,
 * Propriedade antes de Talhão, Talhão antes de Safra, Safra antes de
 * Receita/Despesa/Meta, Máquina antes de Anotação - e Máquina também antes de
 * Despesa, que referencia a máquina opcionalmente), porque cada filho manda o
 * uuid do pai ("propriedadeUuid", "talhaoUuid", "safraUuid", "maquinaUuid")
 * pro servidor resolver o pai -
 * se o pai ainda não tiver chegado lá, o item é pulado nesta passada e tentado
 * de novo na próxima sincronização.
 *
 * Push manda só o que está "sujo" localmente (sincronizado = 0); o servidor
 * aplica "last write wins" comparando o atualizadoEm do cliente com o que ele
 * já tem, e devolve o estado final de cada item pro app reconciliar. Pull traz
 * o que mudou no servidor desde a última sincronização bem-sucedida (marcador
 * guardado no SettingsManager, por usuário).
 */
public class SyncManager {

    public interface Callback {
        void onSucesso();
        void onErro(String mensagem);
    }

    private final ApiService api;
    private final AppDatabase db;
    private final SessionManager session;
    private final SettingsManager settings;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SyncManager(Context context) {
        Context appContext = context.getApplicationContext();
        this.api = RetrofitClient.getApi();
        this.db = AppDatabase.getInstance(appContext);
        this.session = new SessionManager(appContext);
        this.settings = new SettingsManager(appContext);
    }

    /** Chaves de "última sincronização" de cada entidade (por usuário). */
    private static final String[] ENTIDADES = {
            "propriedade", "talhao", "safra", "receita", "despesa", "meta", "maquina", "anotacao_maquina"
    };

    /**
     * Esquece os marcadores de "desde" do usuário: a próxima sincronização baixa tudo de novo
     * (pull completo). Usado na troca de conta do multiusuário, depois de limpar o Room.
     */
    public void esquecerMarcadores() {
        long usuarioId = session.getUsuarioId();
        for (String e : ENTIDADES) settings.remover("ultima_sync_" + e + "_" + usuarioId);
    }

    /**
     * Trava única de sincronização (07/10/2026: com os gatilhos automáticos + o botão manual,
     * duas sincronizações não podem rodar ao mesmo tempo). A troca de conta do multiusuário
     * (apagar dados locais) também passa por ela.
     */
    private static final java.util.concurrent.locks.ReentrantLock TRAVA = new java.util.concurrent.locks.ReentrantLock();

    /** true enquanto alguma sincronização (ou troca de conta) está rodando. */
    public static boolean emAndamento() { return TRAVA.isLocked(); }

    /** Roda o trecho com a trava de sincronização (espera a sincronização atual terminar). Fora da main thread. */
    public static void comTrava(Runnable r) {
        TRAVA.lock();
        try { r.run(); } finally { TRAVA.unlock(); }
    }

    /** Sincroniza tudo que as Fases 1-4 cobrem, nesta ordem (pais antes dos filhos). */
    public void sincronizarTudo(Callback cb) {
        String token = session.getToken();
        long usuarioId = session.getUsuarioId();
        if (token == null || usuarioId == -1L) {
            cb.onErro("Faça login novamente para sincronizar.");
            return;
        }
        final String bearer = "Bearer " + token;

        AppDatabase.IO.execute(() -> {
            // Se outra sincronização estiver rodando (gatilho automático), espera ela terminar.
            TRAVA.lock();
            try {
                sincronizarPropriedades(bearer, usuarioId);
                sincronizarTalhoes(bearer, usuarioId);
                sincronizarSafras(bearer, usuarioId);
                sincronizarMaquinas(bearer, usuarioId);
                sincronizarAnotacoes(bearer, usuarioId);
                reenviarDespesasComMaquinaUmaVez(usuarioId);
                sincronizarReceitas(bearer, usuarioId);
                sincronizarDespesas(bearer, usuarioId);
                sincronizarMetas(bearer, usuarioId);
                // Depois de uma troca de conta os marcadores foram zerados: esta passada já foi o pull completo.
                session.setPullCompletoPendente(false);
                mainHandler.post(cb::onSucesso);
            } catch (SyncException e) {
                mainHandler.post(() -> cb.onErro(e.getMessage()));
            } catch (Exception e) {
                mainHandler.post(() -> cb.onErro("Não foi possível sincronizar. Verifique sua internet e tente novamente."));
            } finally {
                TRAVA.unlock();
            }
        });
    }

    // ------------------------------------------------------------------
    // Propriedade
    // ------------------------------------------------------------------

    private void sincronizarPropriedades(String bearer, long usuarioId) throws Exception {
        PropriedadeDao dao = db.propriedadeDao();

        List<Propriedade> pendentes = dao.listarNaoSincronizadas(usuarioId);
        if (!pendentes.isEmpty()) {
            List<PropriedadeSyncItemDto> itens = new ArrayList<>();
            for (Propriedade p : pendentes) itens.add(paraDto(p));

            Response<List<PropriedadeSyncItemDto>> resp =
                    api.sincronizarPushPropriedades(bearer, itens).execute();
            if (!resp.isSuccessful()) throw new SyncException("Falha ao enviar propriedades para o servidor.");

            List<PropriedadeSyncItemDto> confirmadas = resp.body();
            if (confirmadas != null) {
                for (PropriedadeSyncItemDto dto : confirmadas) {
                    Propriedade local = dao.buscarPorUuid(dto.getUuid());
                    if (local == null) continue;
                    aplicarDto(local, dto);
                    dao.atualizar(local);
                }
            }
        }

        String chave = "ultima_sync_propriedade_" + usuarioId;
        String desde = settings.get(chave, null);
        Response<List<PropriedadeSyncItemDto>> respPull =
                api.sincronizarPullPropriedades(bearer, desde).execute();
        if (!respPull.isSuccessful()) throw new SyncException("Falha ao buscar propriedades do servidor.");

        List<PropriedadeSyncItemDto> doServidor = respPull.body();
        String maiorAtualizadoEm = desde;
        if (doServidor != null) {
            for (PropriedadeSyncItemDto dto : doServidor) {
                Propriedade local = dao.buscarPorUuid(dto.getUuid());
                if (local == null) {
                    Propriedade nova = new Propriedade();
                    nova.uuid = dto.getUuid();
                    nova.usuarioId = usuarioId;
                    nova.criadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
                    aplicarDto(nova, dto);
                    dao.inserir(nova);
                } else {
                    // só sobrescreve o local se o que veio do servidor for mais novo que o local
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.getAtualizadoEm() != null && dto.getAtualizadoEm().compareTo(localIso) > 0) {
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.getAtualizadoEm() != null && (maiorAtualizadoEm == null || dto.getAtualizadoEm().compareTo(maiorAtualizadoEm) > 0)) {
                    maiorAtualizadoEm = dto.getAtualizadoEm();
                }
            }
        }
        if (maiorAtualizadoEm != null) settings.set(chave, maiorAtualizadoEm);
    }

    private PropriedadeSyncItemDto paraDto(Propriedade p) {
        PropriedadeSyncItemDto dto = new PropriedadeSyncItemDto();
        dto.setUuid(p.uuid);
        dto.setNome(p.nome);
        dto.setLocalidade(p.localidade);
        dto.setAreaTotal(p.areaTotal);
        dto.setTipoSolo(p.tipoSolo);
        dto.setMatricula(p.matricula);
        dto.setCib(p.cib);
        dto.setValor(p.valor);
        dto.setObservacoes(p.observacoes);
        dto.setLatitude(p.latitude);
        dto.setLongitude(p.longitude);
        dto.setAtualizadoEm(DateUtils.paraIsoUtc(p.atualizadoEm));
        return dto;
    }

    private void aplicarDto(Propriedade p, PropriedadeSyncItemDto dto) {
        p.nome = dto.getNome();
        p.localidade = dto.getLocalidade();
        p.areaTotal = dto.getAreaTotal();
        p.tipoSolo = dto.getTipoSolo();
        p.matricula = dto.getMatricula();
        p.cib = dto.getCib();
        p.valor = dto.getValor();
        p.observacoes = dto.getObservacoes();
        p.latitude = dto.getLatitude();
        p.longitude = dto.getLongitude();
        p.atualizadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
        p.sincronizado = 1;
        if (p.uuid == null) p.uuid = UUID.randomUUID().toString();
    }

    // ------------------------------------------------------------------
    // Talhão (depende de Propriedade já sincronizada)
    // ------------------------------------------------------------------

    private void sincronizarTalhoes(String bearer, long usuarioId) throws Exception {
        TalhaoDao dao = db.talhaoDao();
        PropriedadeDao propDao = db.propriedadeDao();

        List<Talhao> pendentes = dao.listarNaoSincronizadas(usuarioId);
        if (!pendentes.isEmpty()) {
            List<TalhaoSyncItemDto> itens = new ArrayList<>();
            for (Talhao t : pendentes) {
                Propriedade pai = propDao.buscarPorId(t.propriedadeId);
                if (pai == null) continue; // não deveria acontecer (FK com CASCADE), mas por segurança
                itens.add(paraDto(t, pai.uuid));
            }
            if (!itens.isEmpty()) {
                Response<List<TalhaoSyncItemDto>> resp = api.sincronizarPushTalhoes(bearer, itens).execute();
                if (!resp.isSuccessful()) throw new SyncException("Falha ao enviar talhões para o servidor.");

                List<TalhaoSyncItemDto> confirmados = resp.body();
                if (confirmados != null) {
                    for (TalhaoSyncItemDto dto : confirmados) {
                        Talhao local = dao.buscarPorUuid(dto.getUuid());
                        if (local == null) continue;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
            }
        }

        String chave = "ultima_sync_talhao_" + usuarioId;
        String desde = settings.get(chave, null);
        Response<List<TalhaoSyncItemDto>> respPull = api.sincronizarPullTalhoes(bearer, desde).execute();
        if (!respPull.isSuccessful()) throw new SyncException("Falha ao buscar talhões do servidor.");

        List<TalhaoSyncItemDto> doServidor = respPull.body();
        String maior = desde;
        if (doServidor != null) {
            for (TalhaoSyncItemDto dto : doServidor) {
                Propriedade pai = propDao.buscarPorUuid(dto.getPropriedadeUuid());
                if (pai == null) continue; // propriedade-mãe ainda não chegou localmente; tenta na próxima

                Talhao local = dao.buscarPorUuid(dto.getUuid());
                if (local == null) {
                    Talhao novo = new Talhao();
                    novo.uuid = dto.getUuid();
                    novo.propriedadeId = pai.id;
                    novo.criadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
                    aplicarDto(novo, dto);
                    dao.inserir(novo);
                } else {
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.getAtualizadoEm() != null && dto.getAtualizadoEm().compareTo(localIso) > 0) {
                        local.propriedadeId = pai.id;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.getAtualizadoEm() != null && (maior == null || dto.getAtualizadoEm().compareTo(maior) > 0)) {
                    maior = dto.getAtualizadoEm();
                }
            }
        }
        if (maior != null) settings.set(chave, maior);
    }

    private TalhaoSyncItemDto paraDto(Talhao t, String propriedadeUuid) {
        TalhaoSyncItemDto dto = new TalhaoSyncItemDto();
        dto.setUuid(t.uuid);
        dto.setPropriedadeUuid(propriedadeUuid);
        dto.setNome(t.nome);
        dto.setAreaHa(t.areaHa);
        dto.setCultura(t.cultura);
        dto.setLatitude(t.latitude);
        dto.setLongitude(t.longitude);
        dto.setAtualizadoEm(DateUtils.paraIsoUtc(t.atualizadoEm));
        return dto;
    }

    private void aplicarDto(Talhao t, TalhaoSyncItemDto dto) {
        t.nome = dto.getNome();
        t.areaHa = dto.getAreaHa();
        t.cultura = dto.getCultura();
        t.latitude = dto.getLatitude();
        t.longitude = dto.getLongitude();
        t.atualizadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
        t.sincronizado = 1;
        if (t.uuid == null) t.uuid = UUID.randomUUID().toString();
    }

    // ------------------------------------------------------------------
    // Safra (depende de Talhão já sincronizado)
    // ------------------------------------------------------------------

    private void sincronizarSafras(String bearer, long usuarioId) throws Exception {
        SafraDao dao = db.safraDao();
        TalhaoDao talhaoDao = db.talhaoDao();

        List<Safra> pendentes = dao.listarNaoSincronizadas(usuarioId);
        if (!pendentes.isEmpty()) {
            List<SafraSyncItemDto> itens = new ArrayList<>();
            for (Safra s : pendentes) {
                Talhao pai = talhaoDao.buscarPorId(s.talhaoId);
                if (pai == null) continue; // não deveria acontecer (FK com CASCADE), mas por segurança
                itens.add(paraDto(s, pai.uuid));
            }
            if (!itens.isEmpty()) {
                Response<List<SafraSyncItemDto>> resp = api.sincronizarPushSafras(bearer, itens).execute();
                if (!resp.isSuccessful()) throw new SyncException("Falha ao enviar safras para o servidor.");

                List<SafraSyncItemDto> confirmadas = resp.body();
                if (confirmadas != null) {
                    for (SafraSyncItemDto dto : confirmadas) {
                        Safra local = dao.buscarPorUuid(dto.getUuid());
                        if (local == null) continue;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
            }
        }

        String chave = "ultima_sync_safra_" + usuarioId;
        String desde = settings.get(chave, null);
        Response<List<SafraSyncItemDto>> respPull = api.sincronizarPullSafras(bearer, desde).execute();
        if (!respPull.isSuccessful()) throw new SyncException("Falha ao buscar safras do servidor.");

        List<SafraSyncItemDto> doServidor = respPull.body();
        String maior = desde;
        if (doServidor != null) {
            for (SafraSyncItemDto dto : doServidor) {
                Talhao pai = talhaoDao.buscarPorUuid(dto.getTalhaoUuid());
                if (pai == null) continue; // talhão-pai ainda não chegou localmente; tenta na próxima

                Safra local = dao.buscarPorUuid(dto.getUuid());
                if (local == null) {
                    Safra nova = new Safra();
                    nova.uuid = dto.getUuid();
                    nova.talhaoId = pai.id;
                    nova.criadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
                    aplicarDto(nova, dto);
                    dao.inserir(nova);
                } else {
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.getAtualizadoEm() != null && dto.getAtualizadoEm().compareTo(localIso) > 0) {
                        local.talhaoId = pai.id;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.getAtualizadoEm() != null && (maior == null || dto.getAtualizadoEm().compareTo(maior) > 0)) {
                    maior = dto.getAtualizadoEm();
                }
            }
        }
        if (maior != null) settings.set(chave, maior);
    }

    private SafraSyncItemDto paraDto(Safra s, String talhaoUuid) {
        SafraSyncItemDto dto = new SafraSyncItemDto();
        dto.setUuid(s.uuid);
        dto.setTalhaoUuid(talhaoUuid);
        dto.setVariedade(s.variedade);
        dto.setAnoSafra(s.anoSafra);
        dto.setDataInicio(DateUtils.brParaIso(s.dataInicio));
        dto.setDataFim(DateUtils.brParaIso(s.dataFim));
        dto.setAtualizadoEm(DateUtils.paraIsoUtc(s.atualizadoEm));
        return dto;
    }

    private void aplicarDto(Safra s, SafraSyncItemDto dto) {
        s.variedade = dto.getVariedade();
        s.anoSafra = dto.getAnoSafra();
        // isoParaBr devolve "" (nao null) se a entrada for null - normaliza de volta
        // pra null, que e como o resto do app trata "sem data" (ver nullSeVazio nas telas).
        s.dataInicio = dto.getDataInicio() == null ? null : DateUtils.isoParaBr(dto.getDataInicio());
        s.dataFim = dto.getDataFim() == null ? null : DateUtils.isoParaBr(dto.getDataFim());
        s.atualizadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
        s.sincronizado = 1;
        if (s.uuid == null) s.uuid = UUID.randomUUID().toString();
    }

    // ------------------------------------------------------------------
    // Receita (depende de Safra já sincronizada)
    // ------------------------------------------------------------------

    private void sincronizarReceitas(String bearer, long usuarioId) throws Exception {
        ReceitaDao dao = db.receitaDao();
        SafraDao safraDao = db.safraDao();

        List<Receita> pendentes = dao.listarNaoSincronizadas(usuarioId);
        if (!pendentes.isEmpty()) {
            List<ReceitaSyncItemDto> itens = new ArrayList<>();
            for (Receita r : pendentes) {
                Safra pai = safraDao.buscarPorId(r.safraId);
                if (pai == null) continue;
                itens.add(paraDto(r, pai.uuid));
            }
            if (!itens.isEmpty()) {
                Response<List<ReceitaSyncItemDto>> resp = api.sincronizarPushReceitas(bearer, itens).execute();
                if (!resp.isSuccessful()) throw new SyncException("Falha ao enviar receitas para o servidor.");
                List<ReceitaSyncItemDto> confirmadas = resp.body();
                if (confirmadas != null) {
                    for (ReceitaSyncItemDto dto : confirmadas) {
                        Receita local = dao.buscarPorUuid(dto.getUuid());
                        if (local == null) continue;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
            }
        }

        String chave = "ultima_sync_receita_" + usuarioId;
        String desde = settings.get(chave, null);
        Response<List<ReceitaSyncItemDto>> respPull = api.sincronizarPullReceitas(bearer, desde).execute();
        if (!respPull.isSuccessful()) throw new SyncException("Falha ao buscar receitas do servidor.");

        List<ReceitaSyncItemDto> doServidor = respPull.body();
        String maior = desde;
        if (doServidor != null) {
            for (ReceitaSyncItemDto dto : doServidor) {
                Safra pai = safraDao.buscarPorUuid(dto.getSafraUuid());
                if (pai == null) continue; // safra-mãe ainda não chegou localmente; tenta na próxima

                Receita local = dao.buscarPorUuid(dto.getUuid());
                if (local == null) {
                    Receita nova = new Receita();
                    nova.uuid = dto.getUuid();
                    nova.safraId = pai.id;
                    nova.criadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
                    aplicarDto(nova, dto);
                    dao.inserir(nova);
                } else {
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.getAtualizadoEm() != null && dto.getAtualizadoEm().compareTo(localIso) > 0) {
                        local.safraId = pai.id;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.getAtualizadoEm() != null && (maior == null || dto.getAtualizadoEm().compareTo(maior) > 0)) {
                    maior = dto.getAtualizadoEm();
                }
            }
        }
        if (maior != null) settings.set(chave, maior);
    }

    private ReceitaSyncItemDto paraDto(Receita r, String safraUuid) {
        ReceitaSyncItemDto dto = new ReceitaSyncItemDto();
        dto.setUuid(r.uuid);
        dto.setSafraUuid(safraUuid);
        dto.setDescricao(r.descricao);
        dto.setValor(r.valor);
        dto.setDataRegistro(r.dataRegistro);
        dto.setAtualizadoEm(DateUtils.paraIsoUtc(r.atualizadoEm));
        return dto;
    }

    private void aplicarDto(Receita r, ReceitaSyncItemDto dto) {
        r.descricao = dto.getDescricao();
        r.valor = dto.getValor() == null ? 0 : dto.getValor();
        if (dto.getDataRegistro() != null) r.dataRegistro = dto.getDataRegistro();
        r.atualizadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
        r.sincronizado = 1;
        if (r.uuid == null) r.uuid = UUID.randomUUID().toString();
    }

    // ------------------------------------------------------------------
    // Despesa (depende de Safra; categoria viaja pelo nome, máquina pelo uuid)
    // ------------------------------------------------------------------

    private void sincronizarDespesas(String bearer, long usuarioId) throws Exception {
        DespesaDao dao = db.despesaDao();
        SafraDao safraDao = db.safraDao();

        List<Despesa> pendentes = dao.listarNaoSincronizadas(usuarioId);
        if (!pendentes.isEmpty()) {
            List<DespesaSyncItemDto> itens = new ArrayList<>();
            for (Despesa d : pendentes) {
                Safra pai = safraDao.buscarPorId(d.safraId);
                if (pai == null) continue;
                DespesaSyncItemDto dto = paraDto(d, pai.uuid);
                if (dto.getCategoriaNome() == null) continue; // não deveria acontecer (FK), mas o servidor exige categoria
                itens.add(dto);
            }
            if (!itens.isEmpty()) {
                Response<List<DespesaSyncItemDto>> resp = api.sincronizarPushDespesas(bearer, itens).execute();
                if (!resp.isSuccessful()) throw new SyncException("Falha ao enviar despesas para o servidor.");
                List<DespesaSyncItemDto> confirmadas = resp.body();
                if (confirmadas != null) {
                    for (DespesaSyncItemDto dto : confirmadas) {
                        Despesa local = dao.buscarPorUuid(dto.getUuid());
                        if (local == null) continue;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
            }
        }

        String chave = "ultima_sync_despesa_" + usuarioId;
        String desde = settings.get(chave, null);
        Response<List<DespesaSyncItemDto>> respPull = api.sincronizarPullDespesas(bearer, desde).execute();
        if (!respPull.isSuccessful()) throw new SyncException("Falha ao buscar despesas do servidor.");

        List<DespesaSyncItemDto> doServidor = respPull.body();
        String maior = desde;
        if (doServidor != null) {
            for (DespesaSyncItemDto dto : doServidor) {
                Safra pai = safraDao.buscarPorUuid(dto.getSafraUuid());
                if (pai == null) continue; // safra-mãe ainda não chegou localmente; tenta na próxima

                Despesa local = dao.buscarPorUuid(dto.getUuid());
                if (local == null) {
                    Despesa nova = new Despesa();
                    nova.uuid = dto.getUuid();
                    nova.safraId = pai.id;
                    nova.criadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
                    if (!aplicarDto(nova, dto)) continue; // sem categoria resolvível: não dá pra inserir (FK)
                    dao.inserir(nova);
                } else {
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.getAtualizadoEm() != null && dto.getAtualizadoEm().compareTo(localIso) > 0) {
                        local.safraId = pai.id;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.getAtualizadoEm() != null && (maior == null || dto.getAtualizadoEm().compareTo(maior) > 0)) {
                    maior = dto.getAtualizadoEm();
                }
            }
        }
        if (maior != null) settings.set(chave, maior);
    }

    private DespesaSyncItemDto paraDto(Despesa d, String safraUuid) {
        DespesaSyncItemDto dto = new DespesaSyncItemDto();
        dto.setUuid(d.uuid);
        dto.setSafraUuid(safraUuid);
        CategoriaDespesa cat = db.categoriaDespesaDao().buscarPorId(d.categoriaId);
        dto.setCategoriaNome(cat == null ? null : cat.nome);
        if (d.maquinaId != null) {
            Maquina maq = db.maquinaDao().buscarPorId(d.maquinaId);
            dto.setMaquinaUuid(maq == null ? null : maq.uuid);
        }
        dto.setDescricao(d.descricao);
        dto.setValor(d.valor);
        dto.setDataRegistro(d.dataRegistro);
        dto.setAtualizadoEm(DateUtils.paraIsoUtc(d.atualizadoEm));
        return dto;
    }

    /**
     * Aplica o que veio do servidor. Retorna false se a categoria não pôde ser
     * resolvida (só importa pra despesa nova, que não pode ser inserida sem ela).
     *
     * Máquina: só troca o vínculo local quando o servidor manda um maquinaUuid
     * que existe aqui. maquinaUuid nulo NÃO apaga o vínculo local - antes da
     * Fase 4 o servidor não conhecia as máquinas e devolvia nulo, o que apagaria
     * o vínculo feito no celular. Mantido assim por segurança (limitação
     * conhecida: remover o vínculo em OUTRO aparelho não se propaga pra cá).
     */
    private boolean aplicarDto(Despesa d, DespesaSyncItemDto dto) {
        CategoriaDespesaDao catDao = db.categoriaDespesaDao();
        boolean categoriaOk = true;
        if (dto.getCategoriaNome() != null) {
            CategoriaDespesa cat = catDao.buscarPorNome(dto.getCategoriaNome());
            if (cat == null) {
                // categoria que só existe no servidor: cria localmente com o mesmo nome
                catDao.inserir(new CategoriaDespesa(dto.getCategoriaNome()));
                cat = catDao.buscarPorNome(dto.getCategoriaNome());
            }
            if (cat != null) d.categoriaId = cat.id; else categoriaOk = false;
        } else {
            categoriaOk = false;
        }
        if (dto.getMaquinaUuid() != null) {
            MaquinaDao maqDao = db.maquinaDao();
            Maquina maq = maqDao.buscarPorUuid(dto.getMaquinaUuid());
            if (maq != null) d.maquinaId = maq.id;
        }
        d.descricao = dto.getDescricao();
        d.valor = dto.getValor() == null ? 0 : dto.getValor();
        if (dto.getDataRegistro() != null) d.dataRegistro = dto.getDataRegistro();
        d.atualizadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
        d.sincronizado = 1;
        if (d.uuid == null) d.uuid = UUID.randomUUID().toString();
        return categoriaOk;
    }

    // ------------------------------------------------------------------
    // Meta financeira (depende de Safra já sincronizada)
    // ------------------------------------------------------------------

    private void sincronizarMetas(String bearer, long usuarioId) throws Exception {
        MetaFinanceiraDao dao = db.metaFinanceiraDao();
        SafraDao safraDao = db.safraDao();

        List<MetaFinanceira> pendentes = dao.listarNaoSincronizadas(usuarioId);
        if (!pendentes.isEmpty()) {
            List<MetaFinanceiraSyncItemDto> itens = new ArrayList<>();
            for (MetaFinanceira m : pendentes) {
                Safra pai = safraDao.buscarPorId(m.safraId);
                if (pai == null) continue;
                itens.add(paraDto(m, pai.uuid));
            }
            if (!itens.isEmpty()) {
                Response<List<MetaFinanceiraSyncItemDto>> resp = api.sincronizarPushMetas(bearer, itens).execute();
                if (!resp.isSuccessful()) throw new SyncException("Falha ao enviar metas para o servidor.");
                List<MetaFinanceiraSyncItemDto> confirmadas = resp.body();
                if (confirmadas != null) {
                    for (MetaFinanceiraSyncItemDto dto : confirmadas) {
                        MetaFinanceira local = dao.buscarPorUuid(dto.getUuid());
                        if (local == null) continue;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
            }
        }

        String chave = "ultima_sync_meta_" + usuarioId;
        String desde = settings.get(chave, null);
        Response<List<MetaFinanceiraSyncItemDto>> respPull = api.sincronizarPullMetas(bearer, desde).execute();
        if (!respPull.isSuccessful()) throw new SyncException("Falha ao buscar metas do servidor.");

        List<MetaFinanceiraSyncItemDto> doServidor = respPull.body();
        String maior = desde;
        if (doServidor != null) {
            for (MetaFinanceiraSyncItemDto dto : doServidor) {
                Safra pai = safraDao.buscarPorUuid(dto.getSafraUuid());
                if (pai == null) continue; // safra-mãe ainda não chegou localmente; tenta na próxima

                MetaFinanceira local = dao.buscarPorUuid(dto.getUuid());
                if (local == null) {
                    MetaFinanceira nova = new MetaFinanceira();
                    nova.uuid = dto.getUuid();
                    nova.safraId = pai.id;
                    nova.criadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
                    aplicarDto(nova, dto);
                    dao.inserir(nova);
                } else {
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.getAtualizadoEm() != null && dto.getAtualizadoEm().compareTo(localIso) > 0) {
                        local.safraId = pai.id;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.getAtualizadoEm() != null && (maior == null || dto.getAtualizadoEm().compareTo(maior) > 0)) {
                    maior = dto.getAtualizadoEm();
                }
            }
        }
        if (maior != null) settings.set(chave, maior);
    }

    private MetaFinanceiraSyncItemDto paraDto(MetaFinanceira m, String safraUuid) {
        MetaFinanceiraSyncItemDto dto = new MetaFinanceiraSyncItemDto();
        dto.setUuid(m.uuid);
        dto.setSafraUuid(safraUuid);
        dto.setDescricao(m.descricao);
        dto.setValorAlvo(m.valorAlvo);
        dto.setPrazo(m.prazo);
        dto.setAtualizadoEm(DateUtils.paraIsoUtc(m.atualizadoEm));
        return dto;
    }

    private void aplicarDto(MetaFinanceira m, MetaFinanceiraSyncItemDto dto) {
        m.descricao = dto.getDescricao();
        m.valorAlvo = dto.getValorAlvo() == null ? 0 : dto.getValorAlvo();
        m.prazo = dto.getPrazo();
        m.atualizadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
        m.sincronizado = 1;
        if (m.uuid == null) m.uuid = UUID.randomUUID().toString();
    }

    // ------------------------------------------------------------------
    // Máquina (Fase 4 - pertence direto ao usuário, igual a Propriedade)
    // ------------------------------------------------------------------

    private void sincronizarMaquinas(String bearer, long usuarioId) throws Exception {
        MaquinaDao dao = db.maquinaDao();

        List<Maquina> pendentes = dao.listarNaoSincronizadas(usuarioId);
        if (!pendentes.isEmpty()) {
            List<MaquinaSyncItemDto> itens = new ArrayList<>();
            for (Maquina m : pendentes) itens.add(paraDto(m));

            Response<List<MaquinaSyncItemDto>> resp = api.sincronizarPushMaquinas(bearer, itens).execute();
            if (!resp.isSuccessful()) throw new SyncException("Falha ao enviar máquinas para o servidor.");

            List<MaquinaSyncItemDto> confirmadas = resp.body();
            if (confirmadas != null) {
                for (MaquinaSyncItemDto dto : confirmadas) {
                    Maquina local = dao.buscarPorUuid(dto.getUuid());
                    if (local == null) continue;
                    aplicarDto(local, dto);
                    dao.atualizar(local);
                }
            }
        }

        String chave = "ultima_sync_maquina_" + usuarioId;
        String desde = settings.get(chave, null);
        Response<List<MaquinaSyncItemDto>> respPull = api.sincronizarPullMaquinas(bearer, desde).execute();
        if (!respPull.isSuccessful()) throw new SyncException("Falha ao buscar máquinas do servidor.");

        List<MaquinaSyncItemDto> doServidor = respPull.body();
        String maior = desde;
        if (doServidor != null) {
            for (MaquinaSyncItemDto dto : doServidor) {
                Maquina local = dao.buscarPorUuid(dto.getUuid());
                if (local == null) {
                    Maquina nova = new Maquina();
                    nova.uuid = dto.getUuid();
                    nova.usuarioId = usuarioId;
                    nova.criadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
                    aplicarDto(nova, dto);
                    dao.inserir(nova);
                } else {
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.getAtualizadoEm() != null && dto.getAtualizadoEm().compareTo(localIso) > 0) {
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.getAtualizadoEm() != null && (maior == null || dto.getAtualizadoEm().compareTo(maior) > 0)) {
                    maior = dto.getAtualizadoEm();
                }
            }
        }
        if (maior != null) settings.set(chave, maior);
    }

    private MaquinaSyncItemDto paraDto(Maquina m) {
        MaquinaSyncItemDto dto = new MaquinaSyncItemDto();
        dto.setUuid(m.uuid);
        dto.setNome(m.nome);
        dto.setMarca(m.marca);
        dto.setTipo(m.tipo);
        dto.setModelo(m.modelo);
        dto.setAno(m.ano);
        dto.setIdentificacao(m.identificacao);
        dto.setValor(m.valor);
        dto.setObservacoes(m.observacoes);
        dto.setAtualizadoEm(DateUtils.paraIsoUtc(m.atualizadoEm));
        return dto;
    }

    private void aplicarDto(Maquina m, MaquinaSyncItemDto dto) {
        m.nome = dto.getNome();
        m.marca = dto.getMarca();
        m.tipo = dto.getTipo();
        m.modelo = dto.getModelo();
        m.ano = dto.getAno();
        m.identificacao = dto.getIdentificacao();
        m.valor = dto.getValor();
        m.observacoes = dto.getObservacoes();
        m.atualizadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
        m.sincronizado = 1;
        if (m.uuid == null) m.uuid = UUID.randomUUID().toString();
    }

    // ------------------------------------------------------------------
    // Anotação de máquina (Fase 4 - depende de Máquina já sincronizada)
    // ------------------------------------------------------------------

    private void sincronizarAnotacoes(String bearer, long usuarioId) throws Exception {
        AnotacaoMaquinaDao dao = db.anotacaoMaquinaDao();
        MaquinaDao maqDao = db.maquinaDao();

        List<AnotacaoMaquina> pendentes = dao.listarNaoSincronizadas(usuarioId);
        if (!pendentes.isEmpty()) {
            List<AnotacaoMaquinaSyncItemDto> itens = new ArrayList<>();
            for (AnotacaoMaquina a : pendentes) {
                Maquina pai = maqDao.buscarPorId(a.maquinaId);
                if (pai == null) continue; // não deveria acontecer (FK com CASCADE), mas por segurança
                itens.add(paraDto(a, pai.uuid));
            }
            if (!itens.isEmpty()) {
                Response<List<AnotacaoMaquinaSyncItemDto>> resp = api.sincronizarPushAnotacoes(bearer, itens).execute();
                if (!resp.isSuccessful()) throw new SyncException("Falha ao enviar anotações das máquinas para o servidor.");
                List<AnotacaoMaquinaSyncItemDto> confirmadas = resp.body();
                if (confirmadas != null) {
                    for (AnotacaoMaquinaSyncItemDto dto : confirmadas) {
                        AnotacaoMaquina local = dao.buscarPorUuid(dto.getUuid());
                        if (local == null) continue;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
            }
        }

        String chave = "ultima_sync_anotacao_maquina_" + usuarioId;
        String desde = settings.get(chave, null);
        Response<List<AnotacaoMaquinaSyncItemDto>> respPull = api.sincronizarPullAnotacoes(bearer, desde).execute();
        if (!respPull.isSuccessful()) throw new SyncException("Falha ao buscar anotações das máquinas do servidor.");

        List<AnotacaoMaquinaSyncItemDto> doServidor = respPull.body();
        String maior = desde;
        if (doServidor != null) {
            for (AnotacaoMaquinaSyncItemDto dto : doServidor) {
                Maquina pai = maqDao.buscarPorUuid(dto.getMaquinaUuid());
                if (pai == null) continue; // máquina-mãe ainda não chegou localmente; tenta na próxima

                AnotacaoMaquina local = dao.buscarPorUuid(dto.getUuid());
                if (local == null) {
                    AnotacaoMaquina nova = new AnotacaoMaquina();
                    nova.uuid = dto.getUuid();
                    nova.maquinaId = pai.id;
                    nova.criadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
                    aplicarDto(nova, dto);
                    dao.inserir(nova);
                } else {
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.getAtualizadoEm() != null && dto.getAtualizadoEm().compareTo(localIso) > 0) {
                        local.maquinaId = pai.id;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.getAtualizadoEm() != null && (maior == null || dto.getAtualizadoEm().compareTo(maior) > 0)) {
                    maior = dto.getAtualizadoEm();
                }
            }
        }
        if (maior != null) settings.set(chave, maior);
    }

    private AnotacaoMaquinaSyncItemDto paraDto(AnotacaoMaquina a, String maquinaUuid) {
        AnotacaoMaquinaSyncItemDto dto = new AnotacaoMaquinaSyncItemDto();
        dto.setUuid(a.uuid);
        dto.setMaquinaUuid(maquinaUuid);
        dto.setTexto(a.texto);
        dto.setData((a.data == null || a.data.isEmpty()) ? null : a.data);
        dto.setConcluida(a.concluida == 1);
        dto.setAtualizadoEm(DateUtils.paraIsoUtc(a.atualizadoEm));
        dto.setPrazo((a.prazo == null || a.prazo.isEmpty()) ? null : a.prazo);
        dto.setResponsavelUuid((a.responsavelUuid == null || a.responsavelUuid.isEmpty()) ? null : a.responsavelUuid);
        return dto;
    }

    private void aplicarDto(AnotacaoMaquina a, AnotacaoMaquinaSyncItemDto dto) {
        a.texto = dto.getTexto();
        a.data = dto.getData();
        a.concluida = Boolean.TRUE.equals(dto.getConcluida()) ? 1 : 0;
        a.prazo = dto.getPrazo();
        a.responsavelUuid = dto.getResponsavelUuid();
        a.responsavelNome = dto.getResponsavelNome();
        a.concluidoEm = dto.getConcluidoEm();
        a.concluidoPorNome = dto.getConcluidoPorNome();
        a.criadoPorNome = dto.getCriadoPorNome();
        a.atualizadoEm = DateUtils.deIsoUtc(dto.getAtualizadoEm());
        a.sincronizado = 1;
        if (a.uuid == null) a.uuid = UUID.randomUUID().toString();
    }

    /**
     * Antes da Fase 4 o servidor não conhecia as máquinas, então as despesas já
     * sincronizadas com máquina ficaram SEM máquina no servidor (o app manteve o
     * vínculo local). Na primeira sincronização depois da Fase 4 - já com as
     * máquinas no servidor - marca essas despesas como pendentes pra que o
     * vínculo suba no push de despesas logo em seguida. Roda uma vez só por
     * usuário (flag no SettingsManager).
     */
    private void reenviarDespesasComMaquinaUmaVez(long usuarioId) {
        String flag = "fase4_reenvio_despesas_maquina_" + usuarioId;
        if (settings.get(flag, false)) return;
        db.despesaDao().marcarComMaquinaParaReenvio(usuarioId);
        settings.set(flag, true);
    }
}
