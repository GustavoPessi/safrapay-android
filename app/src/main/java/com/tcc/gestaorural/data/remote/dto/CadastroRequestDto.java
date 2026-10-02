package com.tcc.gestaorural.data.remote.dto;

public class CadastroRequestDto {
    private String nome;
    private String email;
    private String senha;
    private String cpf;
    private String telefone;
    /** Formato ISO (yyyy-MM-dd), ou null. */
    private String nascimento;

    public CadastroRequestDto(String nome, String email, String senha, String cpf, String telefone, String nascimento) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.cpf = cpf;
        this.telefone = telefone;
        this.nascimento = nascimento;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getNascimento() { return nascimento; }
    public void setNascimento(String nascimento) { this.nascimento = nascimento; }
}
