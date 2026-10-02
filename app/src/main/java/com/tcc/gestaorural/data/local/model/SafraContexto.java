package com.tcc.gestaorural.data.local.model;

/** Safra resumida (síncrona) para o assistente de IA: contexto enviado e consultas SAFRAS/AREA. */
public class SafraContexto {
    private String uuid;
    private String cultura;          // talhao.cultura (pode ser null)
    private String anoSafra;
    private String talhaoNome;
    private String talhaoUuid;
    private String propriedadeUuid;
    private String propriedadeNome;
    private Double areaHa;           // área do talhão
    private String dataInicio;       // gravada em dd/MM/yyyy no app (pode ser null)
    private String dataFim;          // null/vazio = safra ativa (em andamento)

    public boolean ativa() { return dataFim == null || dataFim.trim().isEmpty(); }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getCultura() { return cultura; }
    public void setCultura(String cultura) { this.cultura = cultura; }
    public String getAnoSafra() { return anoSafra; }
    public void setAnoSafra(String anoSafra) { this.anoSafra = anoSafra; }
    public String getTalhaoNome() { return talhaoNome; }
    public void setTalhaoNome(String talhaoNome) { this.talhaoNome = talhaoNome; }
    public String getTalhaoUuid() { return talhaoUuid; }
    public void setTalhaoUuid(String talhaoUuid) { this.talhaoUuid = talhaoUuid; }
    public String getPropriedadeUuid() { return propriedadeUuid; }
    public void setPropriedadeUuid(String propriedadeUuid) { this.propriedadeUuid = propriedadeUuid; }
    public String getPropriedadeNome() { return propriedadeNome; }
    public void setPropriedadeNome(String propriedadeNome) { this.propriedadeNome = propriedadeNome; }
    public Double getAreaHa() { return areaHa; }
    public void setAreaHa(Double areaHa) { this.areaHa = areaHa; }
    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }
    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }
}
