package com.tcc.gestaorural.data.remote.dto;

/** Espelha o PerfilResponse do backend. */
public class PerfilResponseDto {
    public String nome;
    public String email;
    public String cpf;
    public String telefone;
    /** Formato ISO (yyyy-MM-dd), ou null. */
    public String nascimento;
    public boolean emailVerificado;
}
