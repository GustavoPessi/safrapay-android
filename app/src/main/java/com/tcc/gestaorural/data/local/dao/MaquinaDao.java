package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.tcc.gestaorural.data.local.entity.Maquina;
import java.util.List;

@Dao
public interface MaquinaDao {
    @Insert long inserir(Maquina m);
    @Update void atualizar(Maquina m);
    @Delete void deletar(Maquina m);

    @Query("SELECT * FROM maquina WHERE usuario_id = :usuarioId ORDER BY nome")
    LiveData<List<Maquina>> observarPorUsuario(long usuarioId);

    @Query("SELECT * FROM maquina WHERE id = :id LIMIT 1")
    LiveData<Maquina> observarPorId(long id);
}
