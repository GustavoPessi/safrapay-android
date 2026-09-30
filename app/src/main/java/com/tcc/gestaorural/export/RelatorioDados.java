package com.tcc.gestaorural.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tudo que os exportadores precisam, já calculado a partir da lista de
 * lançamentos do período: totais, resumo mês a mês e despesas por categoria.
 * Calcular tudo a partir da mesma lista garante que PDF, Excel e CSV batem entre si.
 */
public final class RelatorioDados {

    public static final class Mes {
        public String mes; // yyyy-MM
        public double receita, despesa;
        public double saldo() { return receita - despesa; }
    }

    public static final class Categoria {
        public String nome;
        public double total;
    }

    public String produtor;
    public String periodoIni, periodoFim; // ISO
    public String geradoEm;               // dd/MM/yyyy HH:mm
    public final List<Lancamento> lancamentos = new ArrayList<>();
    public final List<Mes> meses = new ArrayList<>();
    public final List<Categoria> categorias = new ArrayList<>();
    public double totalReceitas, totalDespesas;

    public double saldo() { return totalReceitas - totalDespesas; }

    public String periodoPorExtenso() {
        return Formatos.dataBr(periodoIni) + " a " + Formatos.dataBr(periodoFim);
    }

    public static RelatorioDados montar(String produtor, String ini, String fim,
                                        String geradoEm, List<Lancamento> lancamentos) {
        RelatorioDados r = new RelatorioDados();
        r.produtor = produtor;
        r.periodoIni = ini;
        r.periodoFim = fim;
        r.geradoEm = geradoEm;
        if (lancamentos != null) r.lancamentos.addAll(lancamentos);

        TreeMap<String, Mes> porMes = new TreeMap<>();
        Map<String, Categoria> porCategoria = new LinkedHashMap<>();
        for (Lancamento l : r.lancamentos) {
            String chave = (l.data != null && l.data.length() >= 7) ? l.data.substring(0, 7) : "????-??";
            Mes m = porMes.get(chave);
            if (m == null) {
                m = new Mes();
                m.mes = chave;
                porMes.put(chave, m);
            }
            if (l.ehReceita()) {
                m.receita += l.valor;
                r.totalReceitas += l.valor;
            } else {
                m.despesa += l.valor;
                r.totalDespesas += l.valor;
                String nome = (l.categoria == null || l.categoria.trim().isEmpty()) ? "Sem categoria" : l.categoria;
                Categoria c = porCategoria.get(nome);
                if (c == null) {
                    c = new Categoria();
                    c.nome = nome;
                    porCategoria.put(nome, c);
                }
                c.total += l.valor;
            }
        }
        r.meses.addAll(porMes.values());
        r.categorias.addAll(porCategoria.values());
        Collections.sort(r.categorias, new Comparator<Categoria>() {
            @Override
            public int compare(Categoria a, Categoria b) {
                return Double.compare(b.total, a.total);
            }
        });
        return r;
    }
}
