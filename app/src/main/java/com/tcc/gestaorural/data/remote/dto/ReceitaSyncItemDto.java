package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado no push e no pull de /api/receitas/sync (Fase 3).
 * "safraUuid" identifica a safra-pai. dataRegistro ja e ISO (yyyy-MM-dd) no Room.
 */
public class ReceitaSyncItemDto {
    private String uuid;
    private String safraUuid;
    private String descricao;
    private Double valor;
    private String dataRegistro;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getSafraUuid() { return safraUuid; }
    public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(String dataRegistro) { this.dataRegistro = dataRegistro; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
