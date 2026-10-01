package com.tcc.gestaorural.data.local.model;

/** Safra resumida (síncrona) para o assistente de IA: contexto enviado e consultas SAFRAS/AREA. */
public class SafraContexto {
    public String uuid;
    public String cultura;          // talhao.cultura (pode ser null)
    public String anoSafra;
    public String talhaoNome;
    public String talhaoUuid;
    public String propriedadeUuid;
    public String propriedadeNome;
    public Double areaHa;           // área do talhão
    public String dataInicio;       // gravada em dd/MM/yyyy no app (pode ser null)
    public String dataFim;          // null/vazio = safra ativa (em andamento)

    public boolean ativa() { return dataFim == null || dataFim.trim().isEmpty(); }
}
