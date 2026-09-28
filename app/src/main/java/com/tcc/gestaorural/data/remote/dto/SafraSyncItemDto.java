package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado tanto pro push quanto pro pull de /api/safras/sync.
 * "talhaoUuid" identifica a safra-pai (nunca um id local ou do servidor -
 * eles nunca coincidem entre o celular e o backend). dataInicio/dataFim
 * viajam em ISO (yyyy-MM-dd) - o SyncManager converte pro/do formato de
 * exibicao (dd/MM/yyyy) usado no Room (ver DateUtils.brParaIso/isoParaBr).
 */
public class SafraSyncItemDto {
    public String uuid;
    public String talhaoUuid;
    public String variedade;
    public String anoSafra;
    public String dataInicio;
    public String dataFim;
    public String atualizadoEm;
}
