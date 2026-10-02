package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado no push e no pull de /api/maquinas/sync (Fase 4).
 * Máquina pertence direto ao usuário (não tem pai no sync), igual a Propriedade.
 * "atualizadoEm" é um instante UTC ISO-8601, ver DateUtils.paraIsoUtc/deIsoUtc.
 */
public class MaquinaSyncItemDto {
    private String uuid;
    private String nome;
    private String marca;
    private String tipo;
    private String modelo;
    private String ano;
    private String identificacao;
    private Double valor;
    private String observacoes;
    private String atualizadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getAno() { return ano; }
    public void setAno(String ano) { this.ano = ano; }
    public String getIdentificacao() { return identificacao; }
    public void setIdentificacao(String identificacao) { this.identificacao = identificacao; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
