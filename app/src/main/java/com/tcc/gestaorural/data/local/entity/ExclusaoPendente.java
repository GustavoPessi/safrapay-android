package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Exclusão feita no aparelho que ainda não foi avisada ao servidor (Room v12, 08/10/2026).
 *
 * Excluir continua apagando a linha de verdade no Room (as telas não mudam); antes de
 * apagar, o repositório anota aqui "excluí tal item". O SyncManager manda essas anotações em
 * POST /api/exclusoes/sync e as apaga daqui quando o servidor confirma. Só o item que a
 * pessoa excluiu é anotado: os filhos (talhões, safras, lançamentos, anotações) o servidor
 * exclui em cascata, igual ao Room.
 */
@Entity(tableName = "exclusao_pendente")
public class ExclusaoPendente {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** propriedade, talhao, safra, receita, despesa, meta, maquina ou anotacao_maquina. */
    @NonNull
    public String entidade = "";

    @NonNull
    public String uuid = "";

    /** Quem estava logado ao excluir (só sobe na sincronização dessa conta). */
    @ColumnInfo(name = "usuario_id")
    public long usuarioId;

    /** Instante UTC ISO (só informativo; o servidor grava a hora dele). */
    @ColumnInfo(name = "excluido_em")
    public String excluidoEm;
}
