package com.tcc.gestaorural.data.remote.dto;

/**
 * Formato usado no push e no pull de /api/anotacoes-maquina/sync (Fase 4).
 * "maquinaUuid" identifica a máquina-pai. "data" (opcional) já é ISO
 * (yyyy-MM-dd) no Room. "concluida" viaja como booleano (no Room é 0/1).
 */
public class AnotacaoMaquinaSyncItemDto {
    private String uuid;
    private String maquinaUuid;
    private String texto;
    private String data;
    private Boolean concluida;
    private String atualizadoEm;
    // Multiusuário (06/10/2026): a anotação vira tarefa com ciclo de vida.
    /** yyyy-MM-dd ou null. "Atrasada" não viaja: é concluida = false e prazo < hoje. */
    private String prazo;
    /** uuid do usuário responsável (dono ou membro da equipe); null = sem responsável. */
    private String responsavelUuid;
    // Só no pull (o app guarda os nomes pra exibir; não existe tabela de usuários no Room):
    private String responsavelNome;
    private String concluidoEm;
    private String concluidoPorNome;
    private String criadoPorNome;

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public Boolean getConcluida() { return concluida; }
    public void setConcluida(Boolean concluida) { this.concluida = concluida; }
    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public String getPrazo() { return prazo; }
    public void setPrazo(String prazo) { this.prazo = prazo; }
    public String getResponsavelUuid() { return responsavelUuid; }
    public void setResponsavelUuid(String responsavelUuid) { this.responsavelUuid = responsavelUuid; }
    public String getResponsavelNome() { return responsavelNome; }
    public void setResponsavelNome(String responsavelNome) { this.responsavelNome = responsavelNome; }
    public String getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(String concluidoEm) { this.concluidoEm = concluidoEm; }
    public String getConcluidoPorNome() { return concluidoPorNome; }
    public void setConcluidoPorNome(String concluidoPorNome) { this.concluidoPorNome = concluidoPorNome; }
    public String getCriadoPorNome() { return criadoPorNome; }
    public void setCriadoPorNome(String criadoPorNome) { this.criadoPorNome = criadoPorNome; }
}
