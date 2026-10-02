package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.tcc.gestaorural.data.local.entity.Anexo;

import java.util.List;

@Dao
public interface AnexoDao {
    @Insert long inserir(Anexo a);
    @Delete void deletar(Anexo a);

    /** Comprovante mais recente do lançamento (a Fase 1 mostra um por lançamento). */
    @Query("SELECT * FROM anexo WHERE lancamento_tipo = :tipo AND lancamento_uuid = :lancamentoUuid " +
           "ORDER BY criado_em DESC, id DESC LIMIT 1")
    LiveData<Anexo> observarDoLancamento(String tipo, String lancamentoUuid);

    @Query("SELECT * FROM anexo WHERE lancamento_tipo = :tipo AND lancamento_uuid = :lancamentoUuid " +
           "ORDER BY criado_em DESC, id DESC")
    List<Anexo> listarDoLancamento(String tipo, String lancamentoUuid);

    @Query("SELECT caminho FROM anexo")
    List<String> listarCaminhos();

    /** Anexos cujo lançamento não existe mais (ex.: safra excluída apaga as despesas em cascata). */
    @Query("SELECT * FROM anexo WHERE " +
           "(lancamento_tipo = 'DESPESA' AND lancamento_uuid NOT IN (SELECT uuid FROM despesa)) OR " +
           "(lancamento_tipo = 'RECEITA' AND lancamento_uuid NOT IN (SELECT uuid FROM receita))")
    List<Anexo> listarOrfaos();
}
