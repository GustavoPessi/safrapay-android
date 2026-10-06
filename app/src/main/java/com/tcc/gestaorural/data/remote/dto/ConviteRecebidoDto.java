package com.tcc.gestaorural.data.remote.dto;

/** GET /api/equipe/convites/recebidos: convite pendente pro e-mail da conta. */
public class ConviteRecebidoDto {
    private String uuid;
    private String donoNome;
    private String papel;
    private String criadoEm;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getDonoNome() { return donoNome; }
    public void setDonoNome(String donoNome) { this.donoNome = donoNome; }
    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }
}
