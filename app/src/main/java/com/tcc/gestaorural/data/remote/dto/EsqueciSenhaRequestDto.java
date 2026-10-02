package com.tcc.gestaorural.data.remote.dto;

public class EsqueciSenhaRequestDto {
    private String email;

    public EsqueciSenhaRequestDto(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
