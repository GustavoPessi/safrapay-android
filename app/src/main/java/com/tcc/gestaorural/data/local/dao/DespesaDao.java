package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.model.SaldoLinha;
import com.tcc.gestaorural.data.local.model.MesTotal;
import com.tcc.gestaorural.data.local.model.MaquinaTotal;
import com.tcc.gestaorural.data.local.model.CategoriaTotal;
import com.tcc.gestaorural.data.local.model.TotalPorId;
import java.util.List;

@Dao
public interface DespesaDao {

    @Query("SELECT categoria_despesa.nome AS nome, IFNULL(SUM(despesa.valor),0) AS total " +
           "FROM despesa " +
           "JOIN categoria_despesa ON categoria_despesa.id = despesa.categoria_id " +
           "JOIN safra ON safra.id = despesa.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "AND despesa.data_registro BETWEEN :ini AND :fim " +
           "GROUP BY categoria_despesa.nome")
    LiveData<List<CategoriaTotal>> totaisPorCategoria(long usuarioId, String ini, String fim);
    @Insert long inserir(Despesa d);
    @Update void atualizar(Despesa d);
    @Delete void deletar(Despesa d);

    @Query("SELECT * FROM despesa WHERE safra_id = :safraId ORDER BY data_registro DESC")
    LiveData<List<Despesa>> observarPorSafra(long safraId);

    @Query("SELECT IFNULL(SUM(valor),0) FROM despesa WHERE safra_id = :safraId")
    LiveData<Double> totalPorSafra(long safraId);

    @Query("SELECT * FROM despesa WHERE id = :id LIMIT 1")
    LiveData<Despesa> observarPorId(long id);

    @Query("SELECT * FROM despesa WHERE maquina_id = :maquinaId ORDER BY data_registro DESC")
    LiveData<java.util.List<Despesa>> observarPorMaquina(long maquinaId);

    @Query("SELECT IFNULL(SUM(valor),0) FROM despesa WHERE maquina_id = :maquinaId")
    LiveData<Double> totalPorMaquina(long maquinaId);

    @Query("UPDATE despesa SET maquina_id = NULL WHERE maquina_id = :maquinaId")
    void desvincularMaquina(long maquinaId);

    @Query("SELECT despesa.maquina_id AS maquinaId, IFNULL(SUM(despesa.valor),0) AS total " +
           "FROM despesa " +
           "JOIN safra ON safra.id = despesa.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId AND despesa.maquina_id IS NOT NULL " +
           "GROUP BY despesa.maquina_id")
    LiveData<java.util.List<MaquinaTotal>> totaisPorMaquina(long usuarioId);

    @Query("SELECT despesa.* FROM despesa " +
           "JOIN safra ON safra.id = despesa.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "ORDER BY despesa.data_registro DESC")
    LiveData<java.util.List<Despesa>> observarPorUsuario(long usuarioId);

    @Query("SELECT IFNULL(SUM(despesa.valor),0) FROM despesa " +
           "JOIN safra ON safra.id = despesa.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId")
    LiveData<Double> totalPorUsuario(long usuarioId);

    @Query("SELECT IFNULL(SUM(despesa.valor),0) FROM despesa " +
           "JOIN safra ON safra.id = despesa.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "WHERE talhao.propriedade_id = :propriedadeId")
    LiveData<Double> totalPorPropriedade(long propriedadeId);

    @Query("SELECT despesa.safra_id AS id, IFNULL(SUM(despesa.valor),0) AS total FROM despesa " +
           "JOIN safra ON safra.id = despesa.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId GROUP BY despesa.safra_id")
    LiveData<java.util.List<TotalPorId>> totaisPorSafra(long usuarioId);

    @Query("SELECT talhao.propriedade_id AS propriedadeId, IFNULL(SUM(despesa.valor),0) AS total " +
           "FROM despesa " +
           "JOIN safra ON safra.id = despesa.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "GROUP BY talhao.propriedade_id")
    LiveData<java.util.List<SaldoLinha>> despesasPorPropriedade(long usuarioId);

    @Query("SELECT substr(despesa.data_registro,1,7) AS mes, IFNULL(SUM(despesa.valor),0) AS total " +
           "FROM despesa " +
           "JOIN safra ON safra.id = despesa.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "AND despesa.data_registro BETWEEN :ini AND :fim " +
           "GROUP BY mes ORDER BY mes")
    LiveData<java.util.List<MesTotal>> totaisMensais(long usuarioId, String ini, String fim);
}
