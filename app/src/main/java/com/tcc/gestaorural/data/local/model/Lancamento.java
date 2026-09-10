package com.tcc.gestaorural.data.local.model;

/** Item unificado do extrato financeiro. Não é entidade Room. */
public class Lancamento {
    public static final String TIPO_RECEITA = "RECEITA";
    public static final String TIPO_DESPESA = "DESPESA";

    public long id;
    public String tipo;      // TIPO_RECEITA ou TIPO_DESPESA
    public String descricao;
    public double valor;     // sempre positivo
    public String data;      // ISO yyyy-MM-dd
    public long safraId;

    public boolean isReceita() { return TIPO_RECEITA.equals(tipo); }
}
