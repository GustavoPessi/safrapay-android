package com.tcc.gestaorural.export;

/**
 * Uma linha de lançamento (receita ou despesa) usada na exportação do relatório.
 *
 * Também é o POJO de retorno do RelatorioExportDao (Room): os nomes dos campos
 * batem com os aliases da consulta. Java puro de propósito (sem Android), pra
 * que os geradores de PDF/Excel/CSV possam ser testados fora do app.
 */
public class Lancamento {
    /** "R" = receita, "D" = despesa. */
    public String tipo;
    /** Data do lançamento em ISO (yyyy-MM-dd), igual ao Room. */
    public String data;
    public String descricao;
    public double valor;
    public String propriedade;
    public String talhao;
    public String cultura;
    /** Ano da safra (ex.: "2026/2027"). */
    public String safra;
    /** Só despesa. */
    public String categoria;
    /** Só despesa, opcional. */
    public String maquina;

    public boolean ehReceita() {
        return "R".equals(tipo);
    }

    public String tipoPorExtenso() {
        return ehReceita() ? "Receita" : "Despesa";
    }

    /**
     * Origem resumida: "Soja • 2026/2027 • Talhão Norte" (pula o que estiver
     * vazio). Cultura e safra vêm primeiro porque são o que mais importa quando
     * o texto precisa ser cortado no PDF.
     */
    public String origem() {
        StringBuilder sb = new StringBuilder();
        juntar(sb, cultura);
        juntar(sb, safra);
        juntar(sb, talhao);
        return sb.toString();
    }

    private static void juntar(StringBuilder sb, String parte) {
        if (parte == null || parte.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append(" • ");
        sb.append(parte.trim());
    }
}
