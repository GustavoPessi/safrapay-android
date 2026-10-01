package com.tcc.gestaorural.data.local.dao;

import com.tcc.gestaorural.data.local.model.MetaContexto;
import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.tcc.gestaorural.data.local.entity.MetaFinanceira;
import com.tcc.gestaorural.data.local.model.MetaResumo;
import java.util.List;

@Dao
public interface MetaFinanceiraDao {
    @Insert long inserir(MetaFinanceira m);
    @Update void atualizar(MetaFinanceira m);
    @Delete void deletar(MetaFinanceira m);

    @Query("SELECT * FROM meta_financeira WHERE id = :id LIMIT 1")
    LiveData<MetaFinanceira> observarPorId(long id);

    @Query("SELECT m.id AS metaId, m.descricao AS descricao, m.valor_alvo AS valorAlvo, " +
           "m.prazo AS prazo, m.safra_id AS safraId, " +
           "(IFNULL(talhao.cultura,'Safra') || ' ' || safra.ano_safra) AS safraLabel " +
           "FROM meta_financeira m " +
           "JOIN safra ON safra.id = m.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "ORDER BY m.prazo")
    LiveData<List<MetaResumo>> observarResumoPorUsuario(long usuarioId);

    @Query("SELECT COUNT(*) FROM meta_financeira m " +
           "JOIN safra ON safra.id = m.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId")
    LiveData<Integer> contarPorUsuario(long usuarioId);

    // --- Consultas sincronas usadas pelo SyncManager (roda em background, fora do LiveData) ---

    @Query("SELECT * FROM meta_financeira WHERE uuid = :uuid LIMIT 1")
    MetaFinanceira buscarPorUuid(String uuid);

    @Query("SELECT meta_financeira.* FROM meta_financeira " +
           "JOIN safra ON safra.id = meta_financeira.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId AND meta_financeira.sincronizado = 0")
    List<MetaFinanceira> listarNaoSincronizadas(long usuarioId);

    /** Assistente de IA (consulta METAS): metas do usuario com a safra vinculada. */
    @Query("SELECT m.descricao AS descricao, m.valor_alvo AS valorAlvo, m.prazo AS prazo, " +
           "safra.uuid AS safraUuid, talhao.cultura AS cultura, safra.ano_safra AS anoSafra, " +
           "talhao.nome AS talhaoNome " +
           "FROM meta_financeira m " +
           "JOIN safra ON safra.id = m.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId ORDER BY m.prazo")
    List<MetaContexto> listarContextoSync(long usuarioId);
}
