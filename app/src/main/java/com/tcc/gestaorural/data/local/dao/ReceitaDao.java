package com.tcc.gestaorural.data.local.dao;

import com.tcc.gestaorural.data.local.model.LancamentoLinha;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.tcc.gestaorural.data.local.entity.Receita;
import com.tcc.gestaorural.data.local.model.SaldoLinha;
import com.tcc.gestaorural.data.local.model.MesTotal;
import com.tcc.gestaorural.data.local.model.TotalPorId;
import java.util.List;

@Dao
public interface ReceitaDao {
    @Insert long inserir(Receita r);
    @Update void atualizar(Receita r);
    @Delete void deletar(Receita r);

    @Query("SELECT * FROM receita WHERE safra_id = :safraId ORDER BY data_registro DESC")
    LiveData<List<Receita>> observarPorSafra(long safraId);

    @Query("SELECT IFNULL(SUM(valor),0) FROM receita WHERE safra_id = :safraId")
    LiveData<Double> totalPorSafra(long safraId);

    @Query("SELECT * FROM receita WHERE id = :id LIMIT 1")
    LiveData<Receita> observarPorId(long id);

    @Query("SELECT receita.* FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "ORDER BY receita.data_registro DESC")
    LiveData<java.util.List<Receita>> observarPorUsuario(long usuarioId);

    @Query("SELECT IFNULL(SUM(receita.valor),0) FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId")
    LiveData<Double> totalPorUsuario(long usuarioId);

    @Query("SELECT IFNULL(SUM(receita.valor),0) FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "WHERE talhao.propriedade_id = :propriedadeId")
    LiveData<Double> totalPorPropriedade(long propriedadeId);

    @Query("SELECT receita.safra_id AS id, IFNULL(SUM(receita.valor),0) AS total FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId GROUP BY receita.safra_id")
    LiveData<java.util.List<TotalPorId>> totaisPorSafra(long usuarioId);

    @Query("SELECT talhao.propriedade_id AS propriedadeId, IFNULL(SUM(receita.valor),0) AS total " +
           "FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "GROUP BY talhao.propriedade_id")
    LiveData<java.util.List<SaldoLinha>> receitasPorPropriedade(long usuarioId);

    @Query("SELECT substr(receita.data_registro,1,7) AS mes, IFNULL(SUM(receita.valor),0) AS total " +
           "FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "AND receita.data_registro BETWEEN :ini AND :fim " +
           "GROUP BY mes ORDER BY mes")
    LiveData<java.util.List<MesTotal>> totaisMensais(long usuarioId, String ini, String fim);

    @Query("SELECT IFNULL(SUM(receita.valor),0) FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "AND receita.data_registro BETWEEN :ini AND :fim")
    LiveData<Double> totalNoPeriodo(long usuarioId, String ini, String fim);

    // --- Consultas sincronas usadas pelo SyncManager (roda em background, fora do LiveData) ---

    @Query("SELECT * FROM receita WHERE uuid = :uuid LIMIT 1")
    Receita buscarPorUuid(String uuid);

    @Query("SELECT receita.* FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId AND receita.sincronizado = 0")
    List<Receita> listarNaoSincronizadas(long usuarioId);

    // --- Assistente de IA: consultas (o app calcula; a IA so interpreta a pergunta) ---

    @Query("SELECT 'RECEITA' AS tipo, receita.data_registro AS data, receita.valor AS valor, " +
           "receita.descricao AS descricao, CAST(NULL AS TEXT) AS categoria, safra.uuid AS safraUuid, " +
           "talhao.cultura AS cultura, safra.ano_safra AS anoSafra, talhao.nome AS talhaoNome, " +
           "talhao.uuid AS talhaoUuid, propriedade.uuid AS propriedadeUuid, " +
           "CAST(NULL AS TEXT) AS maquinaUuid, CAST(NULL AS TEXT) AS maquinaNome " +
           "FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId AND receita.data_registro BETWEEN :ini AND :fim " +
           "ORDER BY receita.data_registro DESC")
    List<LancamentoLinha> listarLinhas(long usuarioId, String ini, String fim);

    /** Possivel duplicata: mesma safra, mesmo valor (centavos) e mesma data. */
    @Query("SELECT COUNT(*) FROM receita WHERE safra_id = :safraId AND data_registro = :data " +
           "AND ABS(valor - :valor) < 0.005")
    int contarIguais(long safraId, double valor, String data);
}
