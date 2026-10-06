package com.tcc.gestaorural.data.remote.dto;

/** GET /api/colaborador/maquinas: máquina com a contagem das tarefas que o colaborador pode ver. */
public class MaquinaColaboradorDto {
    private String uuid;
    private String nome;
    private String marca;
    private String modelo;
    private String tipo;
    private Integer tarefasPendentes;
    private Integer tarefasAtrasadas;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getTarefasPendentes() { return tarefasPendentes; }
    public void setTarefasPendentes(Integer tarefasPendentes) { this.tarefasPendentes = tarefasPendentes; }
    public Integer getTarefasAtrasadas() { return tarefasAtrasadas; }
    public void setTarefasAtrasadas(Integer tarefasAtrasadas) { this.tarefasAtrasadas = tarefasAtrasadas; }
}
