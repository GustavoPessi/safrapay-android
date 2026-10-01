package com.tcc.gestaorural.data.local.model;

/** Safra resumida (síncrona) para montar o contexto enviado ao assistente de IA. */
public class SafraContexto {
    public String uuid;
    public String cultura;      // talhao.cultura (pode ser null)
    public String anoSafra;
    public String talhaoNome;
    public String dataFim;      // null/vazio = safra ativa (em andamento)
}
