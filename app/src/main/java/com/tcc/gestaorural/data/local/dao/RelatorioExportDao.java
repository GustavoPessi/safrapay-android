package com.tcc.gestaorural.data.local.dao;

import androidx.room.Dao;
import androidx.room.Query;

import com.tcc.gestaorural.export.Lancamento;

import java.util.List;

/**
 * Consultas síncronas usadas pela exportação do relatório (PDF/Excel/CSV).
 * Rodam em background (AppDatabase.IO), fora do LiveData.
 */
@Dao
public interface RelatorioExportDao {

    /**
     * Receitas e despesas do usuário no período, já com os nomes de
     * propriedade/talhão/cultura/safra/categoria/máquina, em ordem de data.
     * Os aliases batem com os campos da classe Lancamento.
     */
    @Query("SELECT 'R' AS tipo, receita.data_registro AS data, receita.descricao AS descricao, " +
           "receita.valor AS valor, propriedade.nome AS propriedade, talhao.nome AS talhao, " +
           "talhao.cultura AS cultura, safra.ano_safra AS safra, NULL AS categoria, NULL AS maquina " +
           "FROM receita " +
           "JOIN safra ON safra.id = receita.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "AND receita.data_registro BETWEEN :ini AND :fim " +
           "UNION ALL " +
           "SELECT 'D' AS tipo, despesa.data_registro AS data, despesa.descricao AS descricao, " +
           "despesa.valor AS valor, propriedade.nome AS propriedade, talhao.nome AS talhao, " +
           "talhao.cultura AS cultura, safra.ano_safra AS safra, categoria_despesa.nome AS categoria, " +
           "maquina.nome AS maquina " +
           "FROM despesa " +
           "JOIN safra ON safra.id = despesa.safra_id " +
           "JOIN talhao ON talhao.id = safra.talhao_id " +
           "JOIN propriedade ON propriedade.id = talhao.propriedade_id " +
           "LEFT JOIN categoria_despesa ON categoria_despesa.id = despesa.categoria_id " +
           "LEFT JOIN maquina ON maquina.id = despesa.maquina_id " +
           "WHERE propriedade.usuario_id = :usuarioId " +
           "AND despesa.data_registro BETWEEN :ini AND :fim " +
           "ORDER BY data, tipo DESC")
    List<Lancamento> lancamentosNoPeriodo(long usuarioId, String ini, String fim);

    @Query("SELECT nome FROM usuario WHERE id = :usuarioId LIMIT 1")
    String nomeUsuario(long usuarioId);
}
