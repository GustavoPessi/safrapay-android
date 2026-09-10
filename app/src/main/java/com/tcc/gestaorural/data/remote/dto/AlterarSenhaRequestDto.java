package com.tcc.gestaorural.data.remote.dto;

public class AlterarSenhaRequestDto {
    public String senhaAtual;
    public String novaSenha;

    public AlterarSenhaRequestDto(String senhaAtual, String novaSenha) {
        this.senhaAtual = senhaAtual;
        this.novaSenha = novaSenha;
    }
}
