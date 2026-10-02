package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado tanto pro push quanto pro pull de /api/talhoes/sync.
 * "propriedadeUuid" identifica o talhao-pai (nunca um id local ou do servidor -
 * eles nunca coincidem entre o celular e o backend).
 */
public class TalhaoSyncItemDto {
    private String uuid;
    private String propriedadeUuid;
    private String nome;
    private Double areaHa;
    private String cultura;
    private Double latitude;
    private Double longitude;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getPropriedadeUuid() { return propriedadeUuid; }
    public void setPropriedadeUuid(String propriedadeUuid) { this.propriedadeUuid = propriedadeUuid; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Double getAreaHa() { return areaHa; }
    public void setAreaHa(Double areaHa) { this.areaHa = areaHa; }
    public String getCultura() { return cultura; }
    public void setCultura(String cultura) { this.cultura = cultura; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
