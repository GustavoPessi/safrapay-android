package com.tcc.gestaorural.data.remote.dto;

public class RegisterRequestDto {
    public String nome;
    public String email;
    public String senha;
    public String cpf;
    public String telefone;
    /** Formato ISO (yyyy-MM-dd), ou null. */
    public String nascimento;

    public RegisterRequestDto(String nome, String email, String senha, String cpf, String telefone, String nascimento) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.cpf = cpf;
        this.telefone = telefone;
        this.nascimento = nascimento;
    }
}
