package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.data.local.model.SafraOpcao;
import com.tcc.gestaorural.data.local.model.SafraResumo;
import com.tcc.gestaorural.data.local.model.SafraContexto;
import java.util.List;

@Dao
public interface SafraDao {
    @Insert long inserir(Safra s);
    @Update void atualizar(Safra s);
    @Delete void deletar(Safra s);

    @Query("SELECT * FROM safra WHERE id = :id LIMIT 1")
    LiveData<Safra> observarPorId(long id);

    @Query("SELECT safra.id AS id, safra.talhao_id AS talhaoId, talhao.cultura AS culturaNome, " +
           "safra.variedade AS variedade, safra.ano_safra AS anoSafra, " +
           "safra.data_inicio AS dataInicio, safra.data_fim AS dataFim " +
           "FROM safra JOIN talhao ON talhao.id = safra.talhao_id " +
           "WHERE safra.talhao_id = :talhaoId ORDER BY safra.ano_safra DESC")
    LiveData<List<SafraResumo>> observarResumoPorTalhao(long talhaoId);

    @Query("SELECT safra.id AS id, safra.talhao_id AS talhaoId, talhao.cultura AS culturaNome, " +
           "safra.variedade AS variedade, safra.ano_safra AS anoSafra, " +
           "safra.data_inicio AS dataInicio, safra.data_fim AS dataFim " +
           "FROM safra JOIN talhao ON talhao.id = safra.talhao_id WHERE safra.id = :id LIMIT 1")
    LiveData<SafraResumo> observarResumoPorId(long id);

    @Query("SELECT safra.id AS id, talhao.cultura AS cultura, safra.ano_safra AS anoSafra, " +
           "talhao.nome AS talhaoNome " +
           "FROM safra " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "ORDER BY safra.ano_safra DESC, talhao.nome")
    LiveData<List<SafraOpcao>> observarOpcoesPorUsuario(long usuarioId);

    // --- Consultas sincronas usadas pelo SyncManager (roda em background, fora do LiveData) ---

    @Query("SELECT * FROM safra WHERE uuid = :uuid LIMIT 1")
    Safra buscarPorUuid(String uuid);

    @Query("SELECT safra.* FROM safra " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId AND safra.sincronizado = 0")
    List<Safra> listarNaoSincronizadas(long usuarioId);

    @Query("SELECT * FROM safra WHERE id = :id LIMIT 1")
    Safra buscarPorId(long id);

    /** Contexto do assistente de IA: safras do usuário com uuid + cultura/ano/talhão + data_fim (ativa). */
    @Query("SELECT safra.uuid AS uuid, talhao.cultura AS cultura, safra.ano_safra AS anoSafra, " +
           "talhao.nome AS talhaoNome, talhao.uuid AS talhaoUuid, propriedade.uuid AS propriedadeUuid, " +
           "propriedade.nome AS propriedadeNome, talhao.area_ha AS areaHa, " +
           "safra.data_inicio AS dataInicio, safra.data_fim AS dataFim " +
           "FROM safra " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "ORDER BY (safra.data_fim IS NULL) DESC, safra.ano_safra DESC")
    List<SafraContexto> listarContexto(long usuarioId);
}
