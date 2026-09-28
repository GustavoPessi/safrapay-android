package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado no push e no pull de /api/maquinas/sync (Fase 4).
 * Máquina pertence direto ao usuário (não tem pai no sync), igual a Propriedade.
 * "atualizadoEm" é um instante UTC ISO-8601, ver DateUtils.paraIsoUtc/deIsoUtc.
 */
public class MaquinaSyncItemDto {
    public String uuid;
    public String nome;
    public String marca;
    public String tipo;
    public String modelo;
    public String ano;
    public String identificacao;
    public Double valor;
    public String observacoes;
    public String atualizadoEm;
}
