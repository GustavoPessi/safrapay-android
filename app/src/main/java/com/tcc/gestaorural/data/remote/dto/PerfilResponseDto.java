package com.tcc.gestaorural.data.remote.dto;

/** Espelha o PerfilResponse do backend. */
public class PerfilResponseDto {
    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    /** Formato ISO (yyyy-MM-dd), ou null. */
    private String nascimento;
    private boolean emailVerificado;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getNascimento() { return nascimento; }
    public void setNascimento(String nascimento) { this.nascimento = nascimento; }
    public boolean isEmailVerificado() { return emailVerificado; }
    public void setEmailVerificado(boolean emailVerificado) { this.emailVerificado = emailVerificado; }
}
