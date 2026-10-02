package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado no push e no pull de /api/despesas/sync (Fase 3).
 * - "safraUuid": safra-pai.
 * - "categoriaNome": categoria pelo NOME (ids nao coincidem entre celular e servidor).
 * - "maquinaUuid": maquina opcional. Maquina so sincroniza na Fase 4 - ate la o
 *   servidor pode devolver null mesmo com vinculo local, e o SyncManager
 *   preserva o vinculo local nesse caso.
 */
public class DespesaSyncItemDto {
    private String uuid;
    private String safraUuid;
    private String categoriaNome;
    private String maquinaUuid;
    private String descricao;
    private Double valor;
    private String dataRegistro;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getSafraUuid() { return safraUuid; }
    public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
    public String getCategoriaNome() { return categoriaNome; }
    public void setCategoriaNome(String categoriaNome) { this.categoriaNome = categoriaNome; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(String dataRegistro) { this.dataRegistro = dataRegistro; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
