package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado tanto pro push (POST /api/propriedades/sync) quanto pro pull
 * (GET /api/propriedades/sync) - mesmo formato do PropriedadeSyncRequest/Response
 * do backend. "atualizadoEm" e um instante UTC ISO-8601, ver DateUtils.paraIsoUtc/deIsoUtc.
 */
public class PropriedadeSyncItemDto {
    private String uuid;
    private String nome;
    private String localidade;
    private Double areaTotal;
    private String tipoSolo;
    private String matricula;
    private String cib;
    private Double valor;
    private String observacoes;
    private Double latitude;
    private Double longitude;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLocalidade() { return localidade; }
    public void setLocalidade(String localidade) { this.localidade = localidade; }
    public Double getAreaTotal() { return areaTotal; }
    public void setAreaTotal(Double areaTotal) { this.areaTotal = areaTotal; }
    public String getTipoSolo() { return tipoSolo; }
    public void setTipoSolo(String tipoSolo) { this.tipoSolo = tipoSolo; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getCib() { return cib; }
    public void setCib(String cib) { this.cib = cib; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
