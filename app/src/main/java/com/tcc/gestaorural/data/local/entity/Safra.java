package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Safra = talhão + ano. A cultura vem do talhão. */
@Entity(tableName = "safra",
        indices = {@Index(value = "uuid", unique = true), @Index("talhao_id")},
        foreignKeys = @ForeignKey(entity = Talhao.class,
                parentColumns = "id", childColumns = "talhao_id", onDelete = ForeignKey.CASCADE))
public class Safra {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String uuid;
    @ColumnInfo(name = "talhao_id") public long talhaoId;
    public String variedade;              // variedade da semente (ex.: "Intacta RR2") — opcional
    @NonNull @ColumnInfo(name = "ano_safra") public String anoSafra;
    @ColumnInfo(name = "data_inicio") public String dataInicio;
    @ColumnInfo(name = "data_fim") public String dataFim;

    @NonNull @ColumnInfo(name = "criado_em") public String criadoEm;
    @NonNull @ColumnInfo(name = "atualizado_em") public String atualizadoEm;
    public int sincronizado;
}
