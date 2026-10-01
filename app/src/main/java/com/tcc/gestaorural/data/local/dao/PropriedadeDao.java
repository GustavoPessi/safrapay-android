package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tcc.gestaorural.data.local.entity.Propriedade;
import java.util.List;

@Dao
public interface PropriedadeDao {
    @Insert long inserir(Propriedade p);
    @Update void atualizar(Propriedade p);
    @Delete void deletar(Propriedade p);

    @Query("SELECT * FROM propriedade WHERE usuario_id = :usuarioId ORDER BY nome")
    LiveData<List<Propriedade>> observarPorUsuario(long usuarioId);

    @Query("SELECT * FROM propriedade WHERE id = :id LIMIT 1")
    LiveData<Propriedade> observarPorId(long id);

    // --- Consultas sincronas usadas pelo SyncManager (roda em background, fora do LiveData) ---

    @Query("SELECT * FROM propriedade WHERE id = :id LIMIT 1")
    Propriedade buscarPorId(long id);

    @Query("SELECT * FROM propriedade WHERE uuid = :uuid LIMIT 1")
    Propriedade buscarPorUuid(String uuid);

    @Query("SELECT * FROM propriedade WHERE usuario_id = :usuarioId AND sincronizado = 0")
    List<Propriedade> listarNaoSincronizadas(long usuarioId);

    /** Assistente de IA: propriedades do usuario (contexto + consulta AREA). */
    @Query("SELECT * FROM propriedade WHERE usuario_id = :usuarioId ORDER BY nome")
    List<Propriedade> listarPorUsuarioSync(long usuarioId);
}
