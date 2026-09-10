package com.tcc.gestaorural.util;

import java.text.NumberFormat;
import java.util.Locale;

/** Formatação monetária em Real (R$ 1.234,56). */
public final class MoneyUtils {
    private MoneyUtils() {}
    private static final Locale BR = new Locale("pt", "BR");

    public static String formatar(double valor) {
        return NumberFormat.getCurrencyInstance(BR).format(valor);
    }

    /**
     * Converte texto digitado em double. Aceita "1500.50", "1500,50" e "1.500,50".
     * Retorna null se inválido/vazio.
     */
    public static Double parse(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        String t = s.trim();
        if (t.contains(",")) {           // formato BR: ponto = milhar, vírgula = decimal
            t = t.replace(".", "").replace(",", ".");
        }
        try { return Double.parseDouble(t); }
        catch (NumberFormatException e) { return null; }
    }

    /** Formata com sinal explícito (+ receita / - despesa). */
    public static String comSinal(double valor, boolean receita) {
        String base = formatar(Math.abs(valor));
        return (receita ? "+ " : "- ") + base;
    }
}
