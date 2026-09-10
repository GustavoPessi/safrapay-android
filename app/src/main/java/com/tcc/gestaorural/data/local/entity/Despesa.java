package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "despesa",
        indices = {@Index(value = "uuid", unique = true), @Index("safra_id"), @Index("categoria_id"), @Index("maquina_id")},
        foreignKeys = {
            @ForeignKey(entity = Safra.class, parentColumns = "id",
                    childColumns = "safra_id", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = CategoriaDespesa.class, parentColumns = "id",
                    childColumns = "categoria_id")
        })
public class Despesa {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String uuid;
    @ColumnInfo(name = "safra_id") public long safraId;
    @ColumnInfo(name = "categoria_id") public long categoriaId;
    @ColumnInfo(name = "maquina_id") public Long maquinaId;  // opcional: máquina que gerou o gasto
    @NonNull public String descricao;
    public double valor;
    @NonNull @ColumnInfo(name = "data_registro") public String dataRegistro;

    @NonNull @ColumnInfo(name = "criado_em") public String criadoEm;
    @NonNull @ColumnInfo(name = "atualizado_em") public String atualizadoEm;
    public int sincronizado;
}
