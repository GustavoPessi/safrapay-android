package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado no push e no pull de /api/anotacoes-maquina/sync (Fase 4).
 * "maquinaUuid" identifica a máquina-pai. "data" (opcional) já é ISO
 * (yyyy-MM-dd) no Room. "concluida" viaja como booleano (no Room é 0/1).
 */
public class AnotacaoMaquinaSyncItemDto {
    private String uuid;
    private String maquinaUuid;
    private String texto;
    private String data;
    private Boolean concluida;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public Boolean getConcluida() { return concluida; }
    public void setConcluida(Boolean concluida) { this.concluida = concluida; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
