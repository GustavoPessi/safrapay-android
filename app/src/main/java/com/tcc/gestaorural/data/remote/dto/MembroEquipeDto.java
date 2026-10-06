package com.tcc.gestaorural.data.remote.dto;

/** GET /api/equipe (só o dono): convites e membros. status: PENDENTE | ACEITO | RECUSADO. */
public class MembroEquipeDto {
    private String uuid;
    private String email;
    private String nome;
    private String papel;
    private String status;
    private String criadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }
}
