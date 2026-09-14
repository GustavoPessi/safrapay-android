package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado tanto pro push (POST /api/propriedades/sync) quanto pro pull
 * (GET /api/propriedades/sync) - mesmo formato do PropriedadeSyncRequest/Response
 * do backend. "atualizadoEm" e um instante UTC ISO-8601, ver DateUtils.paraIsoUtc/deIsoUtc.
 */
public class PropriedadeSyncItemDto {
    public String uuid;
    public String nome;
    public String localidade;
    public Double areaTotal;
    public String tipoSolo;
    public String matricula;
    public String cib;
    public Double valor;
    public String observacoes;
    public String atualizadoEm;
}
