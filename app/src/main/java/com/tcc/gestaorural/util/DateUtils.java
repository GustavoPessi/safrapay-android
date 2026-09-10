package com.tcc.gestaorural.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {
    private DateUtils() {}
    private static final SimpleDateFormat DT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat BR = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    /** Equivalente ao datetime('now') do SQLite. */
    public static String agora() { return DT.format(new Date()); }

    /** Data de hoje em ISO (yyyy-MM-dd), como o date('now') do SQLite. */
    public static String hojeIso() { return ISO.format(new Date()); }

    /** Data de hoje em formato BR (dd/MM/yyyy). */
    public static String hojeBr() { return BR.format(new Date()); }

    /** Ano-mês atual (yyyy-MM), para filtrar "este mês". */
    public static String anoMesAtual() { return hojeIso().substring(0, 7); }

    /** Data de hoje + N dias, em ISO (yyyy-MM-dd). N pode ser negativo. */
    public static String hojeMaisDias(int dias) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_MONTH, dias);
        return ISO.format(c.getTime());
    }

    /** Converte dd/MM/yyyy -> yyyy-MM-dd. Retorna null se inválido/vazio. */
    public static String brParaIso(String br) {
        if (br == null || br.trim().length() != 10) return null;
        try { return ISO.format(BR.parse(br.trim())); }
        catch (Exception e) { return null; }
    }

    /** Converte yyyy-MM-dd -> dd/MM/yyyy. Retorna o próprio valor se não der. */
    public static String isoParaBr(String iso) {
        if (iso == null) return "";
        try { return BR.format(ISO.parse(iso)); }
        catch (Exception e) { return iso; }
    }
}
