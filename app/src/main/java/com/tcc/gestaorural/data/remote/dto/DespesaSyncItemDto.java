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
    public String uuid;
    public String safraUuid;
    public String categoriaNome;
    public String maquinaUuid;
    public String descricao;
    public Double valor;
    public String dataRegistro;
    public String atualizadoEm;
}
