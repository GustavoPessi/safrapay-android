package com.tcc.gestaorural.data.local.model;

/** Safra com a cultura do talhão (JOIN) — para listas e detalhe. */
public class SafraResumo {
    public long id;
    public long talhaoId;
    public String culturaNome;   // = talhao.cultura
    public String variedade;
    public String anoSafra;
    public String dataInicio;
    public String dataFim;
}
