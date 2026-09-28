package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado no push e no pull de /api/metas-financeiras/sync (Fase 3).
 * "safraUuid" identifica a safra-pai. prazo (opcional) ja e ISO no Room.
 */
public class MetaFinanceiraSyncItemDto {
    public String uuid;
    public String safraUuid;
    public String descricao;
    public Double valorAlvo;
    public String prazo;
    public String atualizadoEm;
}
