package com.tcc.gestaorural.data.remote.dto;

/** Espelha o MensagemResponse do backend (respostas sem corpo estruturado, so texto). */
public class MensagemResponseDto {
    private String mensagem;

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
}
