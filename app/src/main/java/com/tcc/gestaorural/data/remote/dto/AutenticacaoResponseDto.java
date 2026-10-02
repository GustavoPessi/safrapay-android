package com.tcc.gestaorural.data.remote.dto;

/** Espelha o AuthResponse do backend (token JWT + dados basicos do usuario). */
public class AutenticacaoResponseDto {
    private String token;
    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    /** Formato ISO (yyyy-MM-dd), ou null. */
    private String nascimento;
    /** Refresh token (90 dias, muda a cada renovação). Vem no login e no /refresh. */
    private String refreshToken;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
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
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
