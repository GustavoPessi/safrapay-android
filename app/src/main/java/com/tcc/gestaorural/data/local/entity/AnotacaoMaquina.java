package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "anotacao_maquina",
        indices = {@Index(value = "uuid", unique = true), @Index("maquina_id")},
        foreignKeys = @ForeignKey(entity = Maquina.class,
                parentColumns = "id", childColumns = "maquina_id", onDelete = ForeignKey.CASCADE))
public class AnotacaoMaquina {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String uuid;
    @ColumnInfo(name = "maquina_id") public long maquinaId;
    @NonNull public String texto;
    public String data;                   // data da revisão/nota (opcional)
    @ColumnInfo(defaultValue = "0") public int concluida;  // 0 = pendente, 1 = concluída

    // Multiusuário (06/10/2026) — tarefa com ciclo de vida. "Atrasada" é calculada
    // (concluida = 0 e prazo < hoje), não é guardada. Os nomes vêm do servidor no pull
    // (não existe tabela de usuários no Room).
    public String prazo;                                              // yyyy-MM-dd ou null
    @ColumnInfo(name = "responsavel_uuid") public String responsavelUuid;
    @ColumnInfo(name = "responsavel_nome") public String responsavelNome;
    @ColumnInfo(name = "concluido_em") public String concluidoEm;
    @ColumnInfo(name = "concluido_por_nome") public String concluidoPorNome;
    @ColumnInfo(name = "criado_por_nome") public String criadoPorNome;

    @NonNull @ColumnInfo(name = "criado_em") public String criadoEm;
    @NonNull @ColumnInfo(name = "atualizado_em") public String atualizadoEm;
    public int sincronizado;
}
