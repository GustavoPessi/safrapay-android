package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado tanto pro push quanto pro pull de /api/talhoes/sync.
 * "propriedadeUuid" identifica o talhao-pai (nunca um id local ou do servidor -
 * eles nunca coincidem entre o celular e o backend).
 */
public class TalhaoSyncItemDto {
    public String uuid;
    public String propriedadeUuid;
    public String nome;
    public Double areaHa;
    public String cultura;
    public String atualizadoEm;
}
