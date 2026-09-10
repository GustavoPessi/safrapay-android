package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Meta financeira vinculada a uma SAFRA. Progresso = saldo daquela safra. */
@Entity(tableName = "meta_financeira",
        indices = {@Index(value = "uuid", unique = true), @Index("safra_id")},
        foreignKeys = @ForeignKey(entity = Safra.class,
                parentColumns = "id", childColumns = "safra_id", onDelete = ForeignKey.CASCADE))
public class MetaFinanceira {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String uuid;
    @ColumnInfo(name = "safra_id") public long safraId;
    @NonNull public String descricao;
    @ColumnInfo(name = "valor_alvo") public double valorAlvo;
    public String prazo;

    @NonNull @ColumnInfo(name = "criado_em") public String criadoEm;
    @NonNull @ColumnInfo(name = "atualizado_em") public String atualizadoEm;
    public int sincronizado;
}
