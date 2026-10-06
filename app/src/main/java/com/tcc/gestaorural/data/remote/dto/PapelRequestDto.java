package com.tcc.gestaorural.data.remote.dto;

/** PATCH /api/equipe/membros/{uuid}: trocar o papel de um membro. */
public class PapelRequestDto {
    private String papel;
    public PapelRequestDto() {}
    public PapelRequestDto(String papel) { this.papel = papel; }

    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
}
