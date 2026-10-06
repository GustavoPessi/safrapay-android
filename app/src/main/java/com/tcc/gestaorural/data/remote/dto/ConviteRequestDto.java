package com.tcc.gestaorural.data.remote.dto;

/** POST /api/equipe/convites: convidar alguém (papel GERENTE ou COLABORADOR). */
public class ConviteRequestDto {
    private String email;
    private String papel;
    public ConviteRequestDto() {}
    public ConviteRequestDto(String email, String papel) { this.email = email; this.papel = papel; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
}
