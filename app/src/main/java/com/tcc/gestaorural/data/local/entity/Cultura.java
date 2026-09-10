package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Cultura (ex.: Soja, Arroz) — irmã de talhão, vinculada à propriedade. */
@Entity(tableName = "cultura",
        indices = {@Index(value = "uuid", unique = true), @Index("propriedade_id")},
        foreignKeys = @ForeignKey(entity = Propriedade.class,
                parentColumns = "id", childColumns = "propriedade_id", onDelete = ForeignKey.CASCADE))
public class Cultura {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String uuid;
    @ColumnInfo(name = "propriedade_id") public long propriedadeId;
    @NonNull public String nome;

    @NonNull @ColumnInfo(name = "criado_em") public String criadoEm;
    @NonNull @ColumnInfo(name = "atualizado_em") public String atualizadoEm;
    public int sincronizado;
}
