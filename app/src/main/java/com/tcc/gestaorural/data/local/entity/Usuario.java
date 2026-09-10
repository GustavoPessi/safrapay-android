package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela: usuario (espelha schema_sistema_rural.sql). */
@Entity(tableName = "usuario",
        indices = {@Index(value = "uuid", unique = true), @Index(value = "email", unique = true)})
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String uuid;
    @NonNull public String nome;
    @NonNull public String email;

    @NonNull @ColumnInfo(name = "senha_hash") public String senhaHash;

    public String cpf;
    public String telefone;
    public String nascimento;

    @NonNull @ColumnInfo(name = "criado_em") public String criadoEm;
    @NonNull @ColumnInfo(name = "atualizado_em") public String atualizadoEm;
    public int sincronizado;
}
