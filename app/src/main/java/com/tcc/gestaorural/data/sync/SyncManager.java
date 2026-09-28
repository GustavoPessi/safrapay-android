package com.tcc.gestaorural.data.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.PropriedadeDao;
import com.tcc.gestaorural.data.local.dao.SafraDao;
import com.tcc.gestaorural.data.local.dao.TalhaoDao;
import com.tcc.gestaorural.data.local.entity.Propriedade;
import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.data.local.entity.Talhao;
import com.tcc.gestaorural.data.remote.ApiService;
import com.tcc.gestaorural.data.remote.RetrofitClient;
import com.tcc.gestaorural.data.remote.dto.PropriedadeSyncItemDto;
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
 * (Propriedade + Talhão) e a Fase 2 (Safra - adicionada em 28/09/2026); as
 * próximas fases repetem exatamente este padrão pras outras entidades
 * (Receita/Despesa/Meta, Máquina/Anotação).
 *
 * OBS (28/09/2026): a entidade Cultura NÃO faz parte da sincronização - o app
 * abandonou esse modelo em 14/09/2026 (a cultura vem do talhão, campo texto
 * livre em Talhão.cultura, já sincronizado desde a Fase 1). O que existe hoje
 * em data/local/entity/Cultura.java e ui/cultura/ é código órfão, sem tela que
 * navegue até ele - mantido por enquanto, não removido nesta tarefa.
 *
 * Regra de ordem: entidades-pai sincronizam sempre antes das filhas (aqui,
 * Propriedade antes de Talhão, e Talhão antes de Safra), porque o talhão manda
 * "propriedadeUuid" e a safra manda "talhaoUuid" pro servidor resolver o pai -
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

    /** Sincroniza tudo que as Fases 1 e 2 cobrem (Propriedade, Talhão e Safra), nesta ordem. */
    public void sincronizarTudo(Callback cb) {
        String token = session.getToken();
        long usuarioId = session.getUsuarioId();
        if (token == null || usuarioId == -1L) {
            cb.onErro("Faça login novamente para sincronizar.");
            return;
        }
        final String bearer = "Bearer " + token;

        AppDatabase.IO.execute(() -> {
            try {
                sincronizarPropriedades(bearer, usuarioId);
                sincronizarTalhoes(bearer, usuarioId);
                sincronizarSafras(bearer, usuarioId);
                mainHandler.post(cb::onSucesso);
            } catch (SyncException e) {
                mainHandler.post(() -> cb.onErro(e.getMessage()));
            } catch (Exception e) {
                mainHandler.post(() -> cb.onErro("Não foi possível sincronizar. Verifique sua internet e tente novamente."));
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
                    Propriedade local = dao.buscarPorUuid(dto.uuid);
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
                Propriedade local = dao.buscarPorUuid(dto.uuid);
                if (local == null) {
                    Propriedade nova = new Propriedade();
                    nova.uuid = dto.uuid;
                    nova.usuarioId = usuarioId;
                    nova.criadoEm = DateUtils.deIsoUtc(dto.atualizadoEm);
                    aplicarDto(nova, dto);
                    dao.inserir(nova);
                } else {
                    // só sobrescreve o local se o que veio do servidor for mais novo que o local
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.atualizadoEm != null && dto.atualizadoEm.compareTo(localIso) > 0) {
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.atualizadoEm != null && (maiorAtualizadoEm == null || dto.atualizadoEm.compareTo(maiorAtualizadoEm) > 0)) {
                    maiorAtualizadoEm = dto.atualizadoEm;
                }
            }
        }
        if (maiorAtualizadoEm != null) settings.set(chave, maiorAtualizadoEm);
    }

    private PropriedadeSyncItemDto paraDto(Propriedade p) {
        PropriedadeSyncItemDto dto = new PropriedadeSyncItemDto();
        dto.uuid = p.uuid;
        dto.nome = p.nome;
        dto.localidade = p.localidade;
        dto.areaTotal = p.areaTotal;
        dto.tipoSolo = p.tipoSolo;
        dto.matricula = p.matricula;
        dto.cib = p.cib;
        dto.valor = p.valor;
        dto.observacoes = p.observacoes;
        dto.latitude = p.latitude;
        dto.longitude = p.longitude;
        dto.atualizadoEm = DateUtils.paraIsoUtc(p.atualizadoEm);
        return dto;
    }

    private void aplicarDto(Propriedade p, PropriedadeSyncItemDto dto) {
        p.nome = dto.nome;
        p.localidade = dto.localidade;
        p.areaTotal = dto.areaTotal;
        p.tipoSolo = dto.tipoSolo;
        p.matricula = dto.matricula;
        p.cib = dto.cib;
        p.valor = dto.valor;
        p.observacoes = dto.observacoes;
        p.latitude = dto.latitude;
        p.longitude = dto.longitude;
        p.atualizadoEm = DateUtils.deIsoUtc(dto.atualizadoEm);
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
                        Talhao local = dao.buscarPorUuid(dto.uuid);
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
                Propriedade pai = propDao.buscarPorUuid(dto.propriedadeUuid);
                if (pai == null) continue; // propriedade-mãe ainda não chegou localmente; tenta na próxima

                Talhao local = dao.buscarPorUuid(dto.uuid);
                if (local == null) {
                    Talhao novo = new Talhao();
                    novo.uuid = dto.uuid;
                    novo.propriedadeId = pai.id;
                    novo.criadoEm = DateUtils.deIsoUtc(dto.atualizadoEm);
                    aplicarDto(novo, dto);
                    dao.inserir(novo);
                } else {
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.atualizadoEm != null && dto.atualizadoEm.compareTo(localIso) > 0) {
                        local.propriedadeId = pai.id;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.atualizadoEm != null && (maior == null || dto.atualizadoEm.compareTo(maior) > 0)) {
                    maior = dto.atualizadoEm;
                }
            }
        }
        if (maior != null) settings.set(chave, maior);
    }

    private TalhaoSyncItemDto paraDto(Talhao t, String propriedadeUuid) {
        TalhaoSyncItemDto dto = new TalhaoSyncItemDto();
        dto.uuid = t.uuid;
        dto.propriedadeUuid = propriedadeUuid;
        dto.nome = t.nome;
        dto.areaHa = t.areaHa;
        dto.cultura = t.cultura;
        dto.latitude = t.latitude;
        dto.longitude = t.longitude;
        dto.atualizadoEm = DateUtils.paraIsoUtc(t.atualizadoEm);
        return dto;
    }

    private void aplicarDto(Talhao t, TalhaoSyncItemDto dto) {
        t.nome = dto.nome;
        t.areaHa = dto.areaHa;
        t.cultura = dto.cultura;
        t.latitude = dto.latitude;
        t.longitude = dto.longitude;
        t.atualizadoEm = DateUtils.deIsoUtc(dto.atualizadoEm);
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
                        Safra local = dao.buscarPorUuid(dto.uuid);
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
                Talhao pai = talhaoDao.buscarPorUuid(dto.talhaoUuid);
                if (pai == null) continue; // talhão-pai ainda não chegou localmente; tenta na próxima

                Safra local = dao.buscarPorUuid(dto.uuid);
                if (local == null) {
                    Safra nova = new Safra();
                    nova.uuid = dto.uuid;
                    nova.talhaoId = pai.id;
                    nova.criadoEm = DateUtils.deIsoUtc(dto.atualizadoEm);
                    aplicarDto(nova, dto);
                    dao.inserir(nova);
                } else {
                    String localIso = DateUtils.paraIsoUtc(local.atualizadoEm);
                    if (dto.atualizadoEm != null && dto.atualizadoEm.compareTo(localIso) > 0) {
                        local.talhaoId = pai.id;
                        aplicarDto(local, dto);
                        dao.atualizar(local);
                    }
                }
                if (dto.atualizadoEm != null && (maior == null || dto.atualizadoEm.compareTo(maior) > 0)) {
                    maior = dto.atualizadoEm;
                }
            }
        }
        if (maior != null) settings.set(chave, maior);
    }

    private SafraSyncItemDto paraDto(Safra s, String talhaoUuid) {
        SafraSyncItemDto dto = new SafraSyncItemDto();
        dto.uuid = s.uuid;
        dto.talhaoUuid = talhaoUuid;
        dto.variedade = s.variedade;
        dto.anoSafra = s.anoSafra;
        dto.dataInicio = DateUtils.brParaIso(s.dataInicio);
        dto.dataFim = DateUtils.brParaIso(s.dataFim);
        dto.atualizadoEm = DateUtils.paraIsoUtc(s.atualizadoEm);
        return dto;
    }

    private void aplicarDto(Safra s, SafraSyncItemDto dto) {
        s.variedade = dto.variedade;
        s.anoSafra = dto.anoSafra;
        // isoParaBr devolve "" (nao null) se a entrada for null - normaliza de volta
        // pra null, que e como o resto do app trata "sem data" (ver nullSeVazio nas telas).
        s.dataInicio = dto.dataInicio == null ? null : DateUtils.isoParaBr(dto.dataInicio);
        s.dataFim = dto.dataFim == null ? null : DateUtils.isoParaBr(dto.dataFim);
        s.atualizadoEm = DateUtils.deIsoUtc(dto.atualizadoEm);
        s.sincronizado = 1;
        if (s.uuid == null) s.uuid = UUID.randomUUID().toString();
    }
}
