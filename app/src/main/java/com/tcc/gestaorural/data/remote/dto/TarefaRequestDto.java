package com.tcc.gestaorural.data.remote.dto;

/** POST /api/colaborador/maquinas/{uuid}/tarefas: colaborador reporta um problema na máquina. */
public class TarefaRequestDto {
    private String texto;
    public TarefaRequestDto() {}
    public TarefaRequestDto(String texto) { this.texto = texto; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
