package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "talhao",
        indices = {@Index(value = "uuid", unique = true), @Index("propriedade_id")},
        foreignKeys = @ForeignKey(entity = Propriedade.class,
                parentColumns = "id", childColumns = "propriedade_id", onDelete = ForeignKey.CASCADE))
public class Talhao {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String uuid;
    @ColumnInfo(name = "propriedade_id") public long propriedadeId;
    @NonNull public String nome;
    @ColumnInfo(name = "area_ha") public Double areaHa;
    public String cultura;                // cultura do talhão (ex.: Soja) — escolhida da lista de cotações
    public Double latitude;               // coordenada GPS — opcional (Geolocalização Nível 1)
    public Double longitude;              // coordenada GPS — opcional (Geolocalização Nível 1)

    @NonNull @ColumnInfo(name = "criado_em") public String criadoEm;
    @NonNull @ColumnInfo(name = "atualizado_em") public String atualizadoEm;
    public int sincronizado;
}
