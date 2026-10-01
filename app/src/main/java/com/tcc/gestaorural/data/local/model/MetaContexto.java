package com.tcc.gestaorural.data.local.model;

/** Meta financeira com a safra vinculada, para a consulta METAS do assistente de IA. */
public class MetaContexto {
    public String descricao;
    public double valorAlvo;
    public String prazo;            // yyyy-MM-dd (pode ser null)
    public String safraUuid;
    public String cultura;
    public String anoSafra;
    public String talhaoNome;
}
