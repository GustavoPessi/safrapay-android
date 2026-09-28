package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado no push e no pull de /api/anotacoes-maquina/sync (Fase 4).
 * "maquinaUuid" identifica a máquina-pai. "data" (opcional) já é ISO
 * (yyyy-MM-dd) no Room. "concluida" viaja como booleano (no Room é 0/1).
 */
public class AnotacaoMaquinaSyncItemDto {
    public String uuid;
    public String maquinaUuid;
    public String texto;
    public String data;
    public Boolean concluida;
    public String atualizadoEm;
}
