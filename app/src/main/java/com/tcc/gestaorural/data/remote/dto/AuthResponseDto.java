package com.tcc.gestaorural.data.remote.dto;

/** Espelha o AuthResponse do backend (token JWT + dados basicos do usuario). */
public class AuthResponseDto {
    public String token;
    public String nome;
    public String email;
    public String cpf;
    public String telefone;
    /** Formato ISO (yyyy-MM-dd), ou null. */
    public String nascimento;
}
