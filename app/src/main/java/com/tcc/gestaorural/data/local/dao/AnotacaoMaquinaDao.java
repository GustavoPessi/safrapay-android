package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.tcc.gestaorural.data.local.entity.AnotacaoMaquina;
import com.tcc.gestaorural.data.local.model.AvisoManutencao;
import java.util.List;

@Dao
public interface AnotacaoMaquinaDao {
    @Insert long inserir(AnotacaoMaquina a);
    @Update void atualizar(AnotacaoMaquina a);
    @Delete void deletar(AnotacaoMaquina a);

    @Query("SELECT * FROM anotacao_maquina WHERE maquina_id = :maquinaId ORDER BY " +
           "CASE WHEN data IS NULL THEN 1 ELSE 0 END, data DESC, criado_em DESC")
    LiveData<List<AnotacaoMaquina>> observarPorMaquina(long maquinaId);

    /** Avisos de manutenção: pendentes, com data definida, vencidas ou até :limite. */
    @Query("SELECT a.id AS id, a.maquina_id AS maquinaId, a.texto AS texto, a.data AS data, " +
           "maquina.nome AS maquinaNome " +
           "FROM anotacao_maquina a " +
           "JOIN maquina ON maquina.id = a.maquina_id " +
           "WHERE maquina.usuario_id = :usuarioId AND a.concluida = 0 " +
           "AND a.data IS NOT NULL AND a.data <= :limite " +
           "ORDER BY a.data ASC")
    LiveData<List<AvisoManutencao>> avisos(long usuarioId, String limite);

    // --- Consultas sincronas usadas pelo SyncManager (Fase 4, roda em background) ---

    @Query("SELECT * FROM anotacao_maquina WHERE uuid = :uuid LIMIT 1")
    AnotacaoMaquina buscarPorUuid(String uuid);

    @Query("SELECT anotacao_maquina.* FROM anotacao_maquina " +
           "JOIN maquina ON maquina.id = anotacao_maquina.maquina_id " +
           "WHERE maquina.usuario_id = :usuarioId AND anotacao_maquina.sincronizado = 0")
    List<AnotacaoMaquina> listarNaoSincronizadas(long usuarioId);

    /** Assistente de IA (consulta MAQUINAS): anotacoes/manutencoes ainda pendentes do usuario. */
    @Query("SELECT anotacao_maquina.* FROM anotacao_maquina " +
           "JOIN maquina ON maquina.id = anotacao_maquina.maquina_id " +
           "WHERE maquina.usuario_id = :usuarioId AND anotacao_maquina.concluida = 0 " +
           "ORDER BY anotacao_maquina.data")
    List<AnotacaoMaquina> listarPendentesSync(long usuarioId);
}
