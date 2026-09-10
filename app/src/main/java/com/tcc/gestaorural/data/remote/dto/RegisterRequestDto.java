package com.tcc.gestaorural.data.remote.dto;

public class RegisterRequestDto {
    public String nome;
    public String email;
    public String senha;
    public String cpf;
    public String telefone;

    public RegisterRequestDto(String nome, String email, String senha, String cpf, String telefone) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.cpf = cpf;
        this.telefone = telefone;
    }
}
