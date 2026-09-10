package com.tcc.gestaorural.data.local.dao;

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
}
