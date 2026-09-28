package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado no push e no pull de /api/receitas/sync (Fase 3).
 * "safraUuid" identifica a safra-pai. dataRegistro ja e ISO (yyyy-MM-dd) no Room.
 */
public class ReceitaSyncItemDto {
    public String uuid;
    public String safraUuid;
    public String descricao;
    public Double valor;
    public String dataRegistro;
    public String atualizadoEm;
}
