package com.tcc.gestaorural.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateUtils {
    private DateUtils() {}
    private static final SimpleDateFormat DT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat BR = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    // Instante UTC usado na sincronizacao com o backend (padrao fixo de 3 digitos
    // nos milissegundos - o backend formata do mesmo jeito, ver SyncDateUtil la).
    private static final SimpleDateFormat ISO_UTC =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    static {
        ISO_UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

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

    /**
     * Converte um timestamp local (formato usado nas colunas criado_em/atualizado_em,
     * "yyyy-MM-dd HH:mm:ss" no fuso do aparelho) pro instante UTC ISO-8601 usado nos
     * campos "atualizadoEm" da sincronizacao com o backend.
     */
    public static synchronized String paraIsoUtc(String localDbTime) {
        if (localDbTime == null) return ISO_UTC.format(new Date());
        try { return ISO_UTC.format(DT.parse(localDbTime)); }
        catch (Exception e) { return ISO_UTC.format(new Date()); }
    }

    /**
     * Converte um instante UTC ISO-8601 (mandado pelo backend na sincronizacao)
     * pro formato local usado nas colunas criado_em/atualizado_em.
     */
    public static synchronized String deIsoUtc(String isoUtc) {
        if (isoUtc == null) return agora();
        try { return DT.format(ISO_UTC.parse(isoUtc)); }
        catch (Exception e) { return agora(); }
    }

    /**
     * Instante vindo do servidor (ex.: "2026-10-06T14:03:00Z", com ou sem milissegundos) →
     * "06/10/2026" no fuso do aparelho. Se não der pra ler, usa só a parte da data.
     */
    public static synchronized String dataBrDeInstante(String iso) {
        if (iso == null || iso.length() < 10) return "";
        try {
            String t = iso.trim();
            // ISO_UTC espera milissegundos (".SSS") e "Z": completa quando o servidor não manda.
            if (t.endsWith("Z") && !t.contains(".")) t = t.substring(0, t.length() - 1) + ".000Z";
            return BR.format(ISO_UTC.parse(t));
        } catch (Exception e) {
            return isoParaBr(iso.substring(0, 10));
        }
    }

    /** Instante do servidor (UTC ISO) → "08/10 às 14:32" no fuso do aparelho; "" se não der pra ler. */
    public static synchronized String diaHoraDeInstante(String iso) {
        if (iso == null || iso.length() < 16) return "";
        try {
            String t = iso.trim();
            if (t.endsWith("Z") && !t.contains(".")) t = t.substring(0, t.length() - 1) + ".000Z";
            Date d = ISO_UTC.parse(t);
            return new SimpleDateFormat("dd/MM 'às' HH:mm", Locale.US).format(d);
        } catch (Exception e) {
            return "";
        }
    }

    /** Coluna local "yyyy-MM-dd HH:mm:ss" (criado_em do Room) → "08/10 às 14:32"; "" se não der. */
    public static synchronized String diaHoraDeLocal(String local) {
        if (local == null) return "";
        try {
            return new SimpleDateFormat("dd/MM 'às' HH:mm", Locale.US).format(DT.parse(local));
        } catch (Exception e) {
            return "";
        }
    }
}
