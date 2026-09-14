package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.tcc.gestaorural.data.local.entity.Talhao;
import java.util.List;

@Dao
public interface TalhaoDao {
    @Insert long inserir(Talhao t);
    @Update void atualizar(Talhao t);
    @Delete void deletar(Talhao t);

    @Query("SELECT * FROM talhao WHERE propriedade_id = :propriedadeId ORDER BY nome")
    LiveData<List<Talhao>> observarPorPropriedade(long propriedadeId);

    @Query("SELECT * FROM talhao WHERE id = :id LIMIT 1")
    LiveData<Talhao> observarPorId(long id);

    @Query("SELECT IFNULL(SUM(talhao.area_ha),0) FROM talhao " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId")
    LiveData<Double> totalAreaPorUsuario(long usuarioId);

    // --- Consultas sincronas usadas pelo SyncManager (roda em background, fora do LiveData) ---

    @Query("SELECT * FROM talhao WHERE uuid = :uuid LIMIT 1")
    Talhao buscarPorUuid(String uuid);

    @Query("SELECT talhao.* FROM talhao " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId AND talhao.sincronizado = 0")
    List<Talhao> listarNaoSincronizadas(long usuarioId);
}
