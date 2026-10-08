package com.tcc.gestaorural.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.tcc.gestaorural.data.local.entity.ExclusaoPendente;

import java.util.List;

/** Exclusões ainda não avisadas ao servidor (usado pelo SyncManager). */
@Dao
public interface ExclusaoPendenteDao {

    @Insert
    long inserir(ExclusaoPendente e);

    @Query("SELECT * FROM exclusao_pendente WHERE usuario_id = :usuarioId ORDER BY id LIMIT :limite")
    List<ExclusaoPendente> listarDoUsuario(long usuarioId, int limite);

    @Query("DELETE FROM exclusao_pendente WHERE id IN (:ids)")
    void apagar(List<Long> ids);
}
