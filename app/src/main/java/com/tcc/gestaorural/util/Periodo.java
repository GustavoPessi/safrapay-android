package com.tcc.gestaorural.util;

import java.util.Calendar;
import java.util.Locale;

/**
 * Período escolhido no filtro (09/10/2026: "analisar um dia, mês ou ano específico"):
 * TUDO, DIA, MES, ANO ou INTERVALO livre. Calcula início/fim em ISO (yyyy-MM-dd, igual ao
 * data_registro do Room) e o texto pra mostrar. Java puro (sem Android) — dá pra testar fora.
 */
public final class Periodo {

    public enum Tipo { TUDO, DIA, MES, ANO, INTERVALO }

    private static final String[] MESES = {
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    private final Tipo tipo;
    private final int ano, mes, dia;          // referência (mes 1–12) pra DIA/MES/ANO
    private final String iniIso, fimIso;      // só no INTERVALO

    private Periodo(Tipo tipo, int ano, int mes, int dia, String iniIso, String fimIso) {
        this.tipo = tipo; this.ano = ano; this.mes = mes; this.dia = dia;
        this.iniIso = iniIso; this.fimIso = fimIso;
    }

    public static Periodo tudo() { return new Periodo(Tipo.TUDO, 0, 0, 0, null, null); }
    public static Periodo dia(int ano, int mes, int dia) { return new Periodo(Tipo.DIA, ano, mes, dia, null, null); }
    public static Periodo mes(int ano, int mes) { return new Periodo(Tipo.MES, ano, mes, 1, null, null); }
    public static Periodo ano(int ano) { return new Periodo(Tipo.ANO, ano, 1, 1, null, null); }

    /** Intervalo livre (datas ISO); se vierem trocadas, desvira. */
    public static Periodo intervalo(String iniIso, String fimIso) {
        if (iniIso != null && fimIso != null && iniIso.compareTo(fimIso) > 0) { String t = iniIso; iniIso = fimIso; fimIso = t; }
        return new Periodo(Tipo.INTERVALO, 0, 0, 0, iniIso, fimIso);
    }

    /** O mesmo tipo de período, no dia/mês/ano de hoje. */
    public static Periodo atual(Tipo tipo) {
        Calendar c = Calendar.getInstance();
        int a = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH) + 1, d = c.get(Calendar.DAY_OF_MONTH);
        switch (tipo) {
            case DIA: return dia(a, m, d);
            case MES: return mes(a, m);
            case ANO: return ano(a);
            case INTERVALO: return intervalo(String.format(Locale.US, "%04d-%02d-01", a, m), iso(a, m, d));
            default: return tudo();
        }
    }

    /** Recria a partir de tipo + início (ISO) + fim (ISO) — usado pra guardar no Bundle. */
    public static Periodo restaurar(String tipo, String iniIso, String fimIso) {
        try {
            Tipo t = Tipo.valueOf(tipo);
            if (t == Tipo.TUDO) return tudo();
            if (t == Tipo.INTERVALO) return intervalo(iniIso, fimIso);
            int a = Integer.parseInt(iniIso.substring(0, 4));
            int m = Integer.parseInt(iniIso.substring(5, 7));
            int d = Integer.parseInt(iniIso.substring(8, 10));
            if (t == Tipo.DIA) return dia(a, m, d);
            if (t == Tipo.MES) return mes(a, m);
            return ano(a);
        } catch (Exception e) {
            return null;
        }
    }

    public Tipo getTipo() { return tipo; }
    public int getAno() { return ano; }
    public int getMes() { return mes; }
    public int getDia() { return dia; }

    /** Início em ISO; null = sem limite (TUDO). */
    public String inicio() {
        switch (tipo) {
            case DIA: return iso(ano, mes, dia);
            case MES: return iso(ano, mes, 1);
            case ANO: return iso(ano, 1, 1);
            case INTERVALO: return iniIso;
            default: return null;
        }
    }

    /** Fim em ISO (inclusive); null = sem limite (TUDO). */
    public String fim() {
        switch (tipo) {
            case DIA: return iso(ano, mes, dia);
            case MES: return iso(ano, mes, ultimoDia(ano, mes));
            case ANO: return iso(ano, 12, 31);
            case INTERVALO: return fimIso;
            default: return null;
        }
    }

    /** true se a data ISO (yyyy-MM-dd) cai dentro do período. */
    public boolean contem(String dataIso) {
        if (tipo == Tipo.TUDO) return true;
        if (dataIso == null || dataIso.length() < 10) return false;
        String d = dataIso.substring(0, 10), i = inicio(), f = fim();
        return (i == null || d.compareTo(i) >= 0) && (f == null || d.compareTo(f) <= 0);
    }

    /** Anterior/próximo dia, mês ou ano (setas do filtro). TUDO e INTERVALO não andam. */
    public Periodo andar(int passos) {
        Calendar c = Calendar.getInstance();
        c.clear();
        switch (tipo) {
            case DIA: c.set(ano, mes - 1, dia); c.add(Calendar.DAY_OF_MONTH, passos);
                return dia(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
            case MES: c.set(ano, mes - 1, 1); c.add(Calendar.MONTH, passos);
                return mes(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
            case ANO: return ano(ano + passos);
            default: return this;
        }
    }

    public boolean podeAndar() { return tipo == Tipo.DIA || tipo == Tipo.MES || tipo == Tipo.ANO; }

    /** "05/11/2026", "novembro de 2026", "2026", "01/09/2026 a 05/11/2026" ou "Todo o período". */
    public String rotulo() {
        switch (tipo) {
            case DIA: return String.format(Locale.US, "%02d/%02d/%04d", dia, mes, ano);
            case MES: return MESES[mes - 1] + " de " + ano;
            case ANO: return String.valueOf(ano);
            case INTERVALO: return br(iniIso) + " a " + br(fimIso);
            default: return "Todo o período";
        }
    }

    public static String nomeMes(int mes) { return MESES[mes - 1]; }

    private static String iso(int a, int m, int d) { return String.format(Locale.US, "%04d-%02d-%02d", a, m, d); }

    private static String br(String iso) {
        if (iso == null || iso.length() < 10) return "…";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    private static int ultimoDia(int a, int m) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(a, m - 1, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
}
