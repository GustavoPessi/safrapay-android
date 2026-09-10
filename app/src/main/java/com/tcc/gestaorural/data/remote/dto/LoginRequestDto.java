package com.tcc.gestaorural.data.remote.dto;

public class LoginRequestDto {
    public String email;
    public String senha;

    public LoginRequestDto(String email, String senha) {
        this.email = email;
        this.senha = senha;
    }
}
