package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "maquina",
        indices = {@Index(value = "uuid", unique = true), @Index("usuario_id")},
        foreignKeys = @ForeignKey(entity = Usuario.class,
                parentColumns = "id", childColumns = "usuario_id", onDelete = ForeignKey.CASCADE))
public class Maquina {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String uuid;
    @ColumnInfo(name = "usuario_id") public long usuarioId;
    @NonNull public String nome;          // apelido, ex.: "Trator MF 4275"
    public String marca;                  // ex.: MASSEY FERGUSON
    public String tipo;                   // ex.: Trator
    public String modelo;                 // ex.: MF 4275
    public String ano;
    public String identificacao;          // série/placa (opcional)
    public Double valor;                  // valor estimado da máquina — opcional
    public String observacoes;

    @NonNull @ColumnInfo(name = "criado_em") public String criadoEm;
    @NonNull @ColumnInfo(name = "atualizado_em") public String atualizadoEm;
    public int sincronizado;
}
