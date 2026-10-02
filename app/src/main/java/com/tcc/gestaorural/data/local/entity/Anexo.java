package com.tcc.gestaorural.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Comprovante (foto da nota/recibo) de uma despesa ou receita.
 * Fase 1: fica só no celular (arquivo em files/comprovantes). Liga pelo uuid do
 * lançamento (não pelo id local), igual ao sync — assim a Fase 2 (servidor/S3)
 * entra sem mudar o desenho.
 */
@Entity(tableName = "anexo",
        indices = {@Index(value = "uuid", unique = true), @Index("lancamento_uuid")})
public class Anexo {

    public static final String TIPO_DESPESA = "DESPESA";
    public static final String TIPO_RECEITA = "RECEITA";
    public static final String ORIGEM_CHAT_FOTO = "CHAT_FOTO";
    public static final String ORIGEM_MANUAL = "MANUAL";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull private String uuid = "";
    /** DESPESA | RECEITA */
    @NonNull @ColumnInfo(name = "lancamento_tipo") private String lancamentoTipo = "";
    @NonNull @ColumnInfo(name = "lancamento_uuid") private String lancamentoUuid = "";
    /** Caminho absoluto do arquivo no armazenamento interno do app. */
    @NonNull private String caminho = "";
    @NonNull private String mime = "image/jpeg";
    @ColumnInfo(name = "tamanho_bytes") private long tamanhoBytes;
    /** CHAT_FOTO | MANUAL */
    @NonNull private String origem = ORIGEM_MANUAL;
    @NonNull @ColumnInfo(name = "criado_em") private String criadoEm = "";
    @NonNull @ColumnInfo(name = "atualizado_em") private String atualizadoEm = "";
    /** Fica 0 até a Fase 2 (envio pro servidor). */
    private int sincronizado;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    @NonNull public String getUuid() { return uuid; }
    public void setUuid(@NonNull String uuid) { this.uuid = uuid; }
    @NonNull public String getLancamentoTipo() { return lancamentoTipo; }
    public void setLancamentoTipo(@NonNull String lancamentoTipo) { this.lancamentoTipo = lancamentoTipo; }
    @NonNull public String getLancamentoUuid() { return lancamentoUuid; }
    public void setLancamentoUuid(@NonNull String lancamentoUuid) { this.lancamentoUuid = lancamentoUuid; }
    @NonNull public String getCaminho() { return caminho; }
    public void setCaminho(@NonNull String caminho) { this.caminho = caminho; }
    @NonNull public String getMime() { return mime; }
    public void setMime(@NonNull String mime) { this.mime = mime; }
    public long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    @NonNull public String getOrigem() { return origem; }
    public void setOrigem(@NonNull String origem) { this.origem = origem; }
    @NonNull public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(@NonNull String criadoEm) { this.criadoEm = criadoEm; }
    @NonNull public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(@NonNull String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public int getSincronizado() { return sincronizado; }
    public void setSincronizado(int sincronizado) { this.sincronizado = sincronizado; }
}
