package com.tcc.gestaorural.util;

/**
 * Rótulo do período da safra (sugestão do Gustavo, 15/10/2026): se a data de início e a de fim
 * caem em anos diferentes, mostra "2026/2027" (convenção agrícola); senão, o ano da safra como
 * foi cadastrado. É só exibição — a coluna ano_safra continua guardando o que o produtor digitou.
 */
public final class SafraPeriodo {

    private SafraPeriodo() {}

    /** Ano de uma data "dd/MM/yyyy" (como o Room guarda) ou "yyyy-MM-dd"; null se não der. */
    static String ano(String data) {
        if (data == null) return null;
        String d = data.trim();
        if (d.matches("\\d{4}-\\d{2}-\\d{2}")) return d.substring(0, 4);
        if (d.matches("\\d{2}/\\d{2}/\\d{4}")) return d.substring(6, 10);
        return null;
    }

    public static String rotulo(String anoSafra, String dataInicio, String dataFim) {
        String base = anoSafra == null ? "" : anoSafra.trim();
        if (base.contains("/")) return base; // já foi digitado como "2026/2027"
        String ai = ano(dataInicio), af = ano(dataFim);
        if (ai != null && af != null && !ai.equals(af)) return ai + "/" + af;
        return base;
    }

    // --- Mesmo cálculo em SQL, pra usar nas consultas do Room (tabela com o nome "safra") ---

    private static final String ANO_INI =
            "(CASE WHEN safra.data_inicio LIKE '____-__-__' THEN substr(safra.data_inicio, 1, 4) "
            + "ELSE substr(safra.data_inicio, 7, 4) END)";
    private static final String ANO_FIM =
            "(CASE WHEN safra.data_fim LIKE '____-__-__' THEN substr(safra.data_fim, 1, 4) "
            + "ELSE substr(safra.data_fim, 7, 4) END)";

    /** Expressão SQL do rótulo (constante: dá pra usar dentro de @Query). */
    public static final String SQL =
            "(CASE WHEN safra.ano_safra NOT LIKE '%/%' "
            + "AND length(IFNULL(safra.data_inicio, '')) = 10 AND length(IFNULL(safra.data_fim, '')) = 10 "
            + "AND " + ANO_INI + " <> " + ANO_FIM + " "
            + "THEN " + ANO_INI + " || '/' || " + ANO_FIM + " ELSE safra.ano_safra END)";
}
