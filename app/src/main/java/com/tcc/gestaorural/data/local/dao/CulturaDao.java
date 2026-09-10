package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.tcc.gestaorural.data.local.entity.Cultura;
import java.util.List;

@Dao
public interface CulturaDao {
    @Insert long inserir(Cultura c);
    @Update void atualizar(Cultura c);
    @Delete void deletar(Cultura c);

    @Query("SELECT * FROM cultura WHERE propriedade_id = :propriedadeId ORDER BY nome")
    LiveData<List<Cultura>> observarPorPropriedade(long propriedadeId);

    @Query("SELECT * FROM cultura WHERE id = :id LIMIT 1")
    LiveData<Cultura> observarPorId(long id);

    @Query("SELECT cultura.* FROM cultura " +
           "JOIN propriedade ON propriedade.id = cultura.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId ORDER BY cultura.nome")
    LiveData<List<Cultura>> observarPorUsuario(long usuarioId);

    /** Busca (case-insensitive) uma cultura da propriedade pelo nome — usada no "criar na hora". */
    @Query("SELECT * FROM cultura WHERE propriedade_id = :propriedadeId AND nome = :nome COLLATE NOCASE LIMIT 1")
    Cultura buscarPorNome(long propriedadeId, String nome);
}
