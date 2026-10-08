package com.tcc.gestaorural.export;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formatação usada pelos exportadores. Sem java.time de propósito: o app tem
 * minSdk 24 e java.time só existe a partir da API 26.
 */
public final class Formatos {

    /**
     * Aviso de relatório gerencial (pedido do Gustavo em 08/10/2026): vai no PDF e no Excel
     * exportados e na tela DRE. Não vai no CSV (dado puro pra importar em outra ferramenta).
     */
    public static final String AVISO_GERENCIAL = "Este é um relatório gerencial gerado automaticamente pelo SafraPay, destinado ao acompanhamento interno da propriedade. Não constitui documento contábil oficial e não substitui a contabilidade formal exigida por lei. Os valores refletem exclusivamente os lançamentos informados pelo usuário no aplicativo — o SafraPay não se responsabiliza pela exatidão, completude ou adequação fiscal desses dados.";
    public static final String AVISO_GERENCIAL_CURTO = "Relatório gerencial — não é documento contábil oficial.";

    private Formatos() {}

    private static final Locale BR = new Locale("pt", "BR");
    private static final String[] MESES = {
        "jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"
    };

    /** R$ 1.234,56 */
    public static String moeda(double valor) {
        return NumberFormat.getCurrencyInstance(BR).format(valor);
    }

    /** 12,5% */
    public static String percentual(double fracao) {
        NumberFormat nf = NumberFormat.getPercentInstance(BR);
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(1);
        return nf.format(fracao);
    }

    /** yyyy-MM-dd -> dd/MM/yyyy (vazio se inválido). */
    public static String dataBr(String iso) {
        if (iso == null || iso.length() < 10) return "";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    /** yyyy-MM -> "jan/2026". */
    public static String mes(String yyyyMM) {
        try {
            int m = Integer.parseInt(yyyyMM.substring(5, 7));
            return MESES[m - 1] + "/" + yyyyMM.substring(0, 4);
        } catch (Exception e) {
            return yyyyMM;
        }
    }

    /** Número pro CSV no padrão brasileiro, sem separador de milhar: 1234,56 */
    public static String numeroCsv(double valor) {
        return String.format(Locale.US, "%.2f", valor).replace('.', ',');
    }

    /**
     * Data ISO -> número de série do Excel (dias desde 30/12/1899).
     * Retorna -1 se a data for inválida.
     */
    public static long serialExcel(String iso) {
        try {
            int y = Integer.parseInt(iso.substring(0, 4));
            int m = Integer.parseInt(iso.substring(5, 7));
            int d = Integer.parseInt(iso.substring(8, 10));
            return diasDesdeEpoca(y, m, d) + 25569L;
        } catch (Exception e) {
            return -1;
        }
    }

    /** Dias desde 01/01/1970 (algoritmo "days from civil", H. Hinnant). */
    static long diasDesdeEpoca(int y, int m, int d) {
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153L * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }
}
