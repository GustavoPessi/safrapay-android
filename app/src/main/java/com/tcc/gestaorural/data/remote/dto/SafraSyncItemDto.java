package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado tanto pro push quanto pro pull de /api/safras/sync.
 * "talhaoUuid" identifica a safra-pai (nunca um id local ou do servidor -
 * eles nunca coincidem entre o celular e o backend). dataInicio/dataFim
 * viajam em ISO (yyyy-MM-dd) - o SyncManager converte pro/do formato de
 * exibicao (dd/MM/yyyy) usado no Room (ver DateUtils.brParaIso/isoParaBr).
 */
public class SafraSyncItemDto {
    private String uuid;
    private String talhaoUuid;
    private String variedade;
    private String anoSafra;
    private String dataInicio;
    private String dataFim;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getTalhaoUuid() { return talhaoUuid; }
    public void setTalhaoUuid(String talhaoUuid) { this.talhaoUuid = talhaoUuid; }
    public String getVariedade() { return variedade; }
    public void setVariedade(String variedade) { this.variedade = variedade; }
    public String getAnoSafra() { return anoSafra; }
    public void setAnoSafra(String anoSafra) { this.anoSafra = anoSafra; }
    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }
    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
