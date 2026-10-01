package com.tcc.gestaorural.data.local.model;

/** Despesa ou receita com os dados de safra/talhão/máquina, para as consultas do assistente de IA. */
public class LancamentoLinha {
    public String tipo;             // "DESPESA" ou "RECEITA"
    public String data;             // yyyy-MM-dd
    public double valor;
    public String descricao;
    public String categoria;        // só despesa
    public String safraUuid;
    public String cultura;
    public String anoSafra;
    public String talhaoNome;
    public String talhaoUuid;
    public String propriedadeUuid;
    public String maquinaUuid;      // só despesa (opcional)
    public String maquinaNome;
}
