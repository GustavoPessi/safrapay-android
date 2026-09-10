package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "propriedade",
        indices = {@Index(value = "uuid", unique = true), @Index("usuario_id")},
        foreignKeys = @ForeignKey(entity = Usuario.class,
                parentColumns = "id", childColumns = "usuario_id", onDelete = ForeignKey.CASCADE))
public class Propriedade {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String uuid;
    @ColumnInfo(name = "usuario_id") public long usuarioId;
    @NonNull public String nome;
    public String localidade;
    @ColumnInfo(name = "area_total") public Double areaTotal;
    @ColumnInfo(name = "tipo_solo") public String tipoSolo;
    public String matricula;
    public String cib;                    // Cadastro do Imóvel Rural (INCRA/CAFIR) — opcional
    public Double valor;                  // valor estimado da propriedade — opcional
    public String observacoes;

    @NonNull @ColumnInfo(name = "criado_em") public String criadoEm;
    @NonNull @ColumnInfo(name = "atualizado_em") public String atualizadoEm;
    public int sincronizado;
}
