package com.tcc.gestaorural.data.remote.dto;

/** GET /api/colaborador/tarefas: tarefa (anotação de máquina) vista pelo colaborador. "atrasada" já vem calculada. */
public class TarefaDto {
    private String uuid;
    private String maquinaUuid;
    private String maquinaNome;
    private String texto;
    private String data;
    private String prazo;
    private Boolean concluida;
    private Boolean atrasada;
    private Boolean minha;
    private Responsavel responsavel;
    private String criadoPorNome;
    private String concluidoEm;
    private String concluidoPorNome;
    /** Instante UTC (com hora) da criação — ex.: quando o problema foi reportado. */
    private String criadoEm;

    /** Quem ficou responsável (null = sem responsável, qualquer um pode fazer). */
    public static class Responsavel {
        private String uuid;
        private String nome;

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
    public String getMaquinaNome() { return maquinaNome; }
    public void setMaquinaNome(String maquinaNome) { this.maquinaNome = maquinaNome; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getPrazo() { return prazo; }
    public void setPrazo(String prazo) { this.prazo = prazo; }
    public Boolean getConcluida() { return concluida; }
    public void setConcluida(Boolean concluida) { this.concluida = concluida; }
    public Boolean getAtrasada() { return atrasada; }
    public void setAtrasada(Boolean atrasada) { this.atrasada = atrasada; }
    public Boolean getMinha() { return minha; }
    public void setMinha(Boolean minha) { this.minha = minha; }
    public Responsavel getResponsavel() { return responsavel; }
    public void setResponsavel(Responsavel responsavel) { this.responsavel = responsavel; }
    public String getCriadoPorNome() { return criadoPorNome; }
    public void setCriadoPorNome(String criadoPorNome) { this.criadoPorNome = criadoPorNome; }
    public String getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(String concluidoEm) { this.concluidoEm = concluidoEm; }
    public String getConcluidoPorNome() { return concluidoPorNome; }
    public void setConcluidoPorNome(String concluidoPorNome) { this.concluidoPorNome = concluidoPorNome; }
    public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }
}
