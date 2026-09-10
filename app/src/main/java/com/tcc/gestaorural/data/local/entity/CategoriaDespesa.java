package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "categoria_despesa",
        indices = {@Index(value = "nome", unique = true)})
public class CategoriaDespesa {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String nome;

    public CategoriaDespesa() {}
    public CategoriaDespesa(@NonNull String nome) { this.nome = nome; }
}
