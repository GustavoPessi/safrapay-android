package com.tcc.gestaorural.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.AnotacaoMaquinaDao;
import com.tcc.gestaorural.data.local.dao.DespesaDao;
import com.tcc.gestaorural.data.local.dao.MaquinaDao;
import com.tcc.gestaorural.data.local.dao.MetaFinanceiraDao;
import com.tcc.gestaorural.data.local.dao.PropriedadeDao;
import com.tcc.gestaorural.data.local.dao.ReceitaDao;
import com.tcc.gestaorural.data.local.dao.SafraDao;
import com.tcc.gestaorural.data.local.dao.TalhaoDao;
import com.tcc.gestaorural.data.local.entity.AnotacaoMaquina;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.data.local.entity.Propriedade;
import com.tcc.gestaorural.data.local.entity.Talhao;
import com.tcc.gestaorural.data.local.model.LancamentoLinha;
import com.tcc.gestaorural.data.local.model.MetaContexto;
import com.tcc.gestaorural.data.local.model.SafraContexto;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemResponseDto;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Executa no Room as perguntas que a IA identificou (status CONSULTA do contrato v2).
 * A IA só diz O QUE o produtor quer saber (tipo + filtros); quem calcula é o app,
 * então os números são sempre exatos e incluem o que ainda não sincronizou.
 */
public class ChatConsultaExecutor {

    /** Resultado pronto pra exibir no card do chat. */
    public static class Resultado {
        public String titulo;
        public String observacao;
        public final List<String> linhas = new ArrayList<>();
        public String total;
    }

    public interface Callback { void onResultado(Resultado r); }

    private static final String SEM_INICIO = "0000-01-01";
    private static final String SEM_FIM = "9999-12-31";
    private static final int MAX_ITENS = 15;
    private static final Locale PT_BR = new Locale("pt", "BR");

    private final DespesaDao despesaDao;
    private final ReceitaDao receitaDao;
    private final SafraDao safraDao;
    private final PropriedadeDao propriedadeDao;
    private final TalhaoDao talhaoDao;
    private final MetaFinanceiraDao metaDao;
    private final MaquinaDao maquinaDao;
    private final AnotacaoMaquinaDao anotacaoDao;
    private final Handler main = new Handler(Looper.getMainLooper());

    public ChatConsultaExecutor(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        despesaDao = db.despesaDao();
        receitaDao = db.receitaDao();
        safraDao = db.safraDao();
        propriedadeDao = db.propriedadeDao();
        talhaoDao = db.talhaoDao();
        metaDao = db.metaFinanceiraDao();
        maquinaDao = db.maquinaDao();
        anotacaoDao = db.anotacaoMaquinaDao();
    }

    public void executar(long usuarioId, ChatMensagemResponseDto.Consulta c, Callback cb) {
        AppDatabase.IO.execute(() -> {
            Resultado r;
            try {
                r = calcular(usuarioId, c);
            } catch (Exception e) {
                r = new Resultado();
                r.titulo = "Não consegui calcular";
                r.linhas.add("Tive um problema ao buscar esses dados no aparelho. Tente perguntar de outro jeito.");
            }
            final Resultado fr = r;
            main.post(() -> cb.onResultado(fr));
        });
    }

    // ------------------------------------------------------------------

    Resultado calcular(long u, ChatMensagemResponseDto.Consulta c) {
        String tipo = c.tipo == null ? "" : c.tipo.trim().toUpperCase(Locale.ROOT);
        switch (tipo) {
            case "GASTOS":      return lancamentos(u, c, true);
            case "RECEITAS":    return lancamentos(u, c, false);
            case "SALDO":       return saldo(u, c);
            case "LANCAMENTOS": return ultimos(u, c);
            case "SAFRAS":      return safras(u, c);
            case "AREA":        return area(u, c);
            case "METAS":       return metas(u, c);
            case "MAQUINAS":    return maquinas(u, c);
            default:
                Resultado r = new Resultado();
                r.titulo = "Consulta não reconhecida";
                r.linhas.add("Ainda não sei responder esse tipo de pergunta.");
                return r;
        }
    }

    // --- GASTOS / RECEITAS -------------------------------------------

    private Resultado lancamentos(long u, ChatMensagemResponseDto.Consulta c, boolean despesa) {
        Periodo p = periodo(c, true);
        List<LancamentoLinha> lista = filtrar(despesa
                ? despesaDao.listarLinhas(u, p.ini, p.fim)
                : receitaDao.listarLinhas(u, p.ini, p.fim), c);

        Resultado r = new Resultado();
        r.titulo = (despesa ? "Gastos" : "Receitas") + descreverFiltros(u, c);
        r.observacao = p.descricao;
        double total = 0;
        for (LancamentoLinha l : lista) total += l.valor;

        if (lista.isEmpty()) {
            r.linhas.add(despesa ? "Nenhum gasto encontrado." : "Nenhuma receita encontrada.");
            r.total = "Total: " + MoneyUtils.formatar(0);
            return r;
        }
        if (notBlank(c.agrupar)) {
            agrupar(r, lista, c.agrupar);
        } else {
            for (int i = 0; i < lista.size() && i < MAX_ITENS; i++) r.linhas.add(linhaItem(lista.get(i), false));
            if (lista.size() > MAX_ITENS) r.linhas.add("… e mais " + (lista.size() - MAX_ITENS) + " lançamento(s)");
        }
        r.total = "Total: " + MoneyUtils.formatar(total) + "  (" + lista.size() + " lançamento" + (lista.size() == 1 ? "" : "s") + ")";
        return r;
    }

    // --- SALDO --------------------------------------------------------

    private Resultado saldo(long u, ChatMensagemResponseDto.Consulta c) {
        Periodo p = periodo(c, true);
        double rec = soma(filtrar(receitaDao.listarLinhas(u, p.ini, p.fim), c));
        double des = soma(filtrar(despesaDao.listarLinhas(u, p.ini, p.fim), c));
        Resultado r = new Resultado();
        r.titulo = "Resultado" + descreverFiltros(u, c);
        r.observacao = p.descricao;
        r.linhas.add("Receitas: " + MoneyUtils.formatar(rec));
        r.linhas.add("Despesas: " + MoneyUtils.formatar(des));
        double s = rec - des;
        r.total = (s >= 0 ? "Lucro: " : "Prejuízo: ") + MoneyUtils.formatar(Math.abs(s));
        return r;
    }

    // --- LANCAMENTOS (últimos) ----------------------------------------

    private Resultado ultimos(long u, ChatMensagemResponseDto.Consulta c) {
        Periodo p = periodo(c, true);
        List<LancamentoLinha> todos = new ArrayList<>(filtrar(despesaDao.listarLinhas(u, p.ini, p.fim), c));
        todos.addAll(filtrar(receitaDao.listarLinhas(u, p.ini, p.fim), c));
        todos.sort((a, b) -> nn(b.data).compareTo(nn(a.data)));

        Resultado r = new Resultado();
        r.titulo = "Lançamentos" + descreverFiltros(u, c);
        r.observacao = p.descricao;
        if (todos.isEmpty()) { r.linhas.add("Nenhum lançamento encontrado."); return r; }
        int limite = Math.min(20, todos.size());
        for (int i = 0; i < limite; i++) r.linhas.add(linhaItem(todos.get(i), true));
        if (todos.size() > limite) r.linhas.add("… e mais " + (todos.size() - limite) + " lançamento(s)");
        r.total = todos.size() + " lançamento" + (todos.size() == 1 ? "" : "s");
        return r;
    }

    // --- SAFRAS -------------------------------------------------------

    private Resultado safras(long u, ChatMensagemResponseDto.Consulta c) {
        String situacao = c.situacao == null ? "ATIVAS" : c.situacao.trim().toUpperCase(Locale.ROOT);
        List<SafraContexto> todas = safraDao.listarContexto(u);
        boolean variasPropriedades = contarPropriedades(todas) > 1;

        List<SafraContexto> lista = new ArrayList<>();
        for (SafraContexto s : todas) {
            if ("ATIVAS".equals(situacao) && !s.ativa()) continue;
            if ("ENCERRADAS".equals(situacao) && s.ativa()) continue;
            if (c.safraUuid != null && !c.safraUuid.equals(s.uuid)) continue;
            if (c.talhaoUuid != null && !c.talhaoUuid.equals(s.talhaoUuid)) continue;
            if (c.propriedadeUuid != null && !c.propriedadeUuid.equals(s.propriedadeUuid)) continue;
            if (notBlank(c.cultura) && !mesmoTexto(c.cultura, s.cultura)) continue;
            lista.add(s);
        }

        Resultado r = new Resultado();
        r.titulo = ("ENCERRADAS".equals(situacao) ? "Safras encerradas"
                : "TODAS".equals(situacao) ? "Todas as safras" : "Safras em aberto")
                + (notBlank(c.cultura) ? " • " + c.cultura : "");
        if (lista.isEmpty()) {
            r.linhas.add("ENCERRADAS".equals(situacao) ? "Nenhuma safra encerrada."
                    : "TODAS".equals(situacao) ? "Nenhuma safra cadastrada."
                    : "Nenhuma safra em aberto no momento. Para abrir uma, use Início → + Nova safra.");
            return r;
        }
        for (SafraContexto s : lista) {
            StringBuilder sb = new StringBuilder();
            sb.append("🌱 ").append(culturaOuPadrao(s.cultura)).append(" • ").append(nn(s.anoSafra));
            if (notBlank(s.talhaoNome)) sb.append(" — Talhão ").append(s.talhaoNome);
            if (variasPropriedades && notBlank(s.propriedadeNome)) sb.append(" (").append(s.propriedadeNome).append(")");
            sb.append("\n     Início: ").append(notBlank(s.dataInicio) ? s.dataInicio : "não informado");
            sb.append("  •  Fim: ").append(s.ativa() ? "em andamento" : s.dataFim);
            r.linhas.add(sb.toString());
        }
        r.total = lista.size() + " safra" + (lista.size() == 1 ? "" : "s");
        return r;
    }

    // --- AREA ---------------------------------------------------------

    private Resultado area(long u, ChatMensagemResponseDto.Consulta c) {
        List<Propriedade> props = propriedadeDao.listarPorUsuarioSync(u);
        List<Talhao> tals = talhaoDao.listarPorUsuarioSync(u);
        Map<Long, Propriedade> porId = new HashMap<>();
        for (Propriedade p : props) porId.put(p.id, p);

        Resultado r = new Resultado();
        if (props.isEmpty()) { r.titulo = "Área"; r.linhas.add("Nenhuma propriedade cadastrada."); return r; }

        if (notBlank(c.cultura) || c.talhaoUuid != null) {
            double soma = 0; int semArea = 0;
            r.titulo = notBlank(c.cultura) ? "Área de " + c.cultura : "Área do talhão";
            for (Talhao t : tals) {
                if (c.talhaoUuid != null && !c.talhaoUuid.equals(t.uuid)) continue;
                if (notBlank(c.cultura) && !mesmoTexto(c.cultura, t.cultura)) continue;
                Propriedade p = porId.get(t.propriedadeId);
                if (c.propriedadeUuid != null && (p == null || !c.propriedadeUuid.equals(p.uuid))) continue;
                if (t.areaHa == null) semArea++; else soma += t.areaHa;
                r.linhas.add("Talhão " + t.nome + (p != null && props.size() > 1 ? " (" + p.nome + ")" : "")
                        + ": " + (t.areaHa == null ? "área não informada" : ha(t.areaHa)));
            }
            if (r.linhas.isEmpty()) r.linhas.add("Nenhum talhão encontrado com esse filtro.");
            r.total = "Total: " + ha(soma) + (semArea > 0 ? "  (" + semArea + " sem área informada)" : "");
            return r;
        }

        r.titulo = "Área das propriedades";
        double total = 0;
        Map<String, Double> porCultura = new LinkedHashMap<>();
        for (Propriedade p : props) {
            if (c.propriedadeUuid != null && !c.propriedadeUuid.equals(p.uuid)) continue;
            double somaTalhoes = 0; int n = 0;
            for (Talhao t : tals) {
                if (t.propriedadeId != p.id) continue;
                n++;
                if (t.areaHa != null) {
                    somaTalhoes += t.areaHa;
                    String cult = culturaOuPadrao(t.cultura);
                    porCultura.put(cult, (porCultura.containsKey(cult) ? porCultura.get(cult) : 0d) + t.areaHa);
                }
            }
            double areaProp = p.areaTotal != null ? p.areaTotal : somaTalhoes;
            total += areaProp;
            r.linhas.add("🏡 " + p.nome + ": " + (p.areaTotal != null ? ha(p.areaTotal) : "área total não informada")
                    + "\n     " + n + " talhão(ões) somando " + ha(somaTalhoes));
        }
        if (!porCultura.isEmpty()) {
            StringBuilder sb = new StringBuilder("Por cultura: ");
            boolean primeiro = true;
            for (Map.Entry<String, Double> e : porCultura.entrySet()) {
                if (!primeiro) sb.append(", ");
                sb.append(e.getKey()).append(" ").append(ha(e.getValue()));
                primeiro = false;
            }
            r.linhas.add(sb.toString());
        }
        r.total = "Área total: " + ha(total);
        return r;
    }

    // --- METAS --------------------------------------------------------

    private Resultado metas(long u, ChatMensagemResponseDto.Consulta c) {
        List<MetaContexto> metas = metaDao.listarContextoSync(u);
        Map<String, Double> saldoPorSafra = new HashMap<>();
        for (LancamentoLinha l : receitaDao.listarLinhas(u, SEM_INICIO, SEM_FIM)) somar(saldoPorSafra, l.safraUuid, l.valor);
        for (LancamentoLinha l : despesaDao.listarLinhas(u, SEM_INICIO, SEM_FIM)) somar(saldoPorSafra, l.safraUuid, -l.valor);

        Resultado r = new Resultado();
        r.titulo = "Metas" + (notBlank(c.cultura) ? " • " + c.cultura : "");
        int n = 0;
        for (MetaContexto m : metas) {
            if (c.safraUuid != null && !c.safraUuid.equals(m.safraUuid)) continue;
            if (notBlank(c.cultura) && !mesmoTexto(c.cultura, m.cultura)) continue;
            double atual = Math.max(0, saldoPorSafra.containsKey(m.safraUuid) ? saldoPorSafra.get(m.safraUuid) : 0);
            int pct = m.valorAlvo > 0 ? (int) Math.min(100, Math.round(atual / m.valorAlvo * 100)) : 0;
            StringBuilder sb = new StringBuilder("🎯 ").append(m.descricao)
                    .append(" (").append(culturaOuPadrao(m.cultura)).append(" ").append(nn(m.anoSafra)).append(")")
                    .append("\n     ").append(MoneyUtils.formatar(atual)).append(" de ").append(MoneyUtils.formatar(m.valorAlvo))
                    .append(" — ").append(pct).append("%");
            if (notBlank(m.prazo)) sb.append(" • prazo ").append(DateUtils.isoParaBr(m.prazo));
            r.linhas.add(sb.toString());
            n++;
        }
        if (n == 0) r.linhas.add("Nenhuma meta encontrada. Você pode criar uma na aba Metas.");
        else r.total = n + " meta" + (n == 1 ? "" : "s") + "  (progresso = receitas − despesas da safra)";
        return r;
    }

    // --- MAQUINAS -----------------------------------------------------

    private Resultado maquinas(long u, ChatMensagemResponseDto.Consulta c) {
        List<Maquina> maqs = maquinaDao.listarPorUsuarioSync(u);
        Periodo p = periodo(c, false);
        Map<String, Double> gastoPorMaquina = new HashMap<>();
        for (LancamentoLinha l : despesaDao.listarLinhas(u, p.ini, p.fim)) {
            if (l.maquinaUuid != null) somar(gastoPorMaquina, l.maquinaUuid, l.valor);
        }
        Map<Long, List<AnotacaoMaquina>> pendentes = new HashMap<>();
        for (AnotacaoMaquina a : anotacaoDao.listarPendentesSync(u)) {
            if (!pendentes.containsKey(a.maquinaId)) pendentes.put(a.maquinaId, new ArrayList<>());
            pendentes.get(a.maquinaId).add(a);
        }

        Resultado r = new Resultado();
        r.titulo = "Máquinas";
        r.observacao = p.descricao;
        if (maqs.isEmpty()) { r.linhas.add("Nenhuma máquina cadastrada."); return r; }
        double total = 0;
        for (Maquina m : maqs) {
            if (c.maquinaUuid != null && !c.maquinaUuid.equals(m.uuid)) continue;
            double g = gastoPorMaquina.containsKey(m.uuid) ? gastoPorMaquina.get(m.uuid) : 0;
            total += g;
            List<AnotacaoMaquina> pend = pendentes.containsKey(m.id) ? pendentes.get(m.id) : new ArrayList<>();
            StringBuilder sb = new StringBuilder("🚜 ").append(m.nome).append(": ").append(MoneyUtils.formatar(g)).append(" em gastos");
            sb.append(pend.isEmpty() ? " • sem pendências"
                    : " • " + pend.size() + " pendente" + (pend.size() == 1 ? "" : "s"));
            for (int i = 0; i < pend.size() && i < 3; i++) {
                AnotacaoMaquina a = pend.get(i);
                sb.append("\n     ⏳ ").append(a.texto);
                if (notBlank(a.data)) sb.append(" (").append(a.data).append(")");
            }
            r.linhas.add(sb.toString());
        }
        if (r.linhas.isEmpty()) r.linhas.add("Máquina não encontrada.");
        r.total = "Total com máquinas: " + MoneyUtils.formatar(total);
        return r;
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private static class Periodo { String ini, fim, descricao; }

    /**
     * Resolve o período da consulta. Sem datas: se não há escopo (safra/cultura/talhão)
     * e o tipo usa o padrão, vale o mês atual (regra do contrato); senão, todo o período.
     */
    private Periodo periodo(ChatMensagemResponseDto.Consulta c, boolean padraoMesAtual) {
        Periodo p = new Periodo();
        String ini = notBlank(c.dataInicio) ? c.dataInicio.trim() : null;
        String fim = notBlank(c.dataFim) ? c.dataFim.trim() : null;
        if (ini == null && fim == null) {
            boolean temEscopo = c.safraUuid != null || c.talhaoUuid != null || notBlank(c.cultura);
            if (padraoMesAtual && !temEscopo) {
                p.ini = DateUtils.anoMesAtual() + "-01";
                p.fim = DateUtils.hojeIso();
                p.descricao = "Neste mês (" + DateUtils.isoParaBr(p.ini) + " a " + DateUtils.isoParaBr(p.fim) + ")";
            } else {
                p.ini = SEM_INICIO; p.fim = SEM_FIM;
                p.descricao = "Todo o período";
            }
            return p;
        }
        if (ini != null && fim != null && ini.compareTo(fim) > 0) { String t = ini; ini = fim; fim = t; }
        p.ini = ini == null ? SEM_INICIO : ini;
        p.fim = fim == null ? SEM_FIM : fim;
        if (ini != null && ini.equals(fim)) p.descricao = "Em " + DateUtils.isoParaBr(ini);
        else if (ini != null && fim != null) p.descricao = "De " + DateUtils.isoParaBr(ini) + " a " + DateUtils.isoParaBr(fim);
        else if (ini != null) p.descricao = "A partir de " + DateUtils.isoParaBr(ini);
        else p.descricao = "Até " + DateUtils.isoParaBr(fim);
        return p;
    }

    private List<LancamentoLinha> filtrar(List<LancamentoLinha> origem, ChatMensagemResponseDto.Consulta c) {
        List<LancamentoLinha> out = new ArrayList<>();
        for (LancamentoLinha l : origem) {
            if (c.safraUuid != null && !c.safraUuid.equals(l.safraUuid)) continue;
            if (c.talhaoUuid != null && !c.talhaoUuid.equals(l.talhaoUuid)) continue;
            if (c.propriedadeUuid != null && !c.propriedadeUuid.equals(l.propriedadeUuid)) continue;
            if (notBlank(c.cultura) && !mesmoTexto(c.cultura, l.cultura)) continue;
            if (notBlank(c.categoria) && !mesmoTexto(c.categoria, l.categoria)) continue;
            if (c.maquinaUuid != null && !c.maquinaUuid.equals(l.maquinaUuid)) continue;
            out.add(l);
        }
        return out;
    }

    private void agrupar(Resultado r, List<LancamentoLinha> lista, String agrupar) {
        String modo = agrupar.trim().toUpperCase(Locale.ROOT);
        Map<String, Double> grupos = new LinkedHashMap<>();
        for (LancamentoLinha l : lista) {
            String chave;
            switch (modo) {
                case "CATEGORIA": chave = notBlank(l.categoria) ? l.categoria : "Sem categoria"; break;
                case "SAFRA":     chave = culturaOuPadrao(l.cultura) + " " + nn(l.anoSafra)
                                          + (notBlank(l.talhaoNome) ? " — Talhão " + l.talhaoNome : ""); break;
                case "MES":       chave = l.data != null && l.data.length() >= 7
                                          ? l.data.substring(5, 7) + "/" + l.data.substring(0, 4) : "Sem data"; break;
                case "MAQUINA":   chave = notBlank(l.maquinaNome) ? l.maquinaNome : "Sem máquina"; break;
                default:          chave = "Total";
            }
            somar(grupos, chave, l.valor);
        }
        List<Map.Entry<String, Double>> ordenado = new ArrayList<>(grupos.entrySet());
        if (!"MES".equals(modo)) ordenado.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        else ordenado.sort((a, b) -> chaveMes(a.getKey()).compareTo(chaveMes(b.getKey())));
        for (Map.Entry<String, Double> e : ordenado) r.linhas.add(e.getKey() + " — " + MoneyUtils.formatar(e.getValue()));
    }

    private String descreverFiltros(long u, ChatMensagemResponseDto.Consulta c) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(c.categoria)) sb.append(" • ").append(c.categoria);
        if (notBlank(c.cultura)) sb.append(" • ").append(c.cultura);
        if (c.safraUuid != null) {
            for (SafraContexto s : safraDao.listarContexto(u)) {
                if (c.safraUuid.equals(s.uuid)) { sb.append(" • ").append(culturaOuPadrao(s.cultura)).append(" ").append(nn(s.anoSafra)); break; }
            }
        }
        if (c.maquinaUuid != null) {
            Maquina m = maquinaDao.buscarPorUuid(c.maquinaUuid);
            if (m != null) sb.append(" • ").append(m.nome);
        }
        if (c.talhaoUuid != null) {
            Talhao t = talhaoDao.buscarPorUuid(c.talhaoUuid);
            if (t != null) sb.append(" • Talhão ").append(t.nome);
        }
        if (c.propriedadeUuid != null) {
            Propriedade p = propriedadeDao.buscarPorUuid(c.propriedadeUuid);
            if (p != null) sb.append(" • ").append(p.nome);
        }
        return sb.toString();
    }

    private static String linhaItem(LancamentoLinha l, boolean comTipo) {
        StringBuilder sb = new StringBuilder();
        sb.append(l.data != null && l.data.length() >= 10 ? l.data.substring(8, 10) + "/" + l.data.substring(5, 7) : "--/--");
        if (comTipo) sb.append(" • ").append("DESPESA".equals(l.tipo) ? "Despesa" : "Receita");
        sb.append(" • ").append(notBlank(l.descricao) ? l.descricao : "(sem descrição)");
        if (notBlank(l.categoria)) sb.append(" (").append(l.categoria).append(")");
        sb.append(" — ").append(MoneyUtils.formatar(l.valor));
        return sb.toString();
    }

    private static int contarPropriedades(List<SafraContexto> safras) {
        java.util.Set<String> s = new java.util.HashSet<>();
        for (SafraContexto x : safras) if (x.propriedadeUuid != null) s.add(x.propriedadeUuid);
        return s.size();
    }

    private static double soma(List<LancamentoLinha> l) { double t = 0; for (LancamentoLinha x : l) t += x.valor; return t; }

    private static void somar(Map<String, Double> m, String k, double v) {
        if (k == null) return;
        m.put(k, (m.containsKey(k) ? m.get(k) : 0d) + v);
    }

    private static String chaveMes(String mmYyyy) {
        return mmYyyy.length() == 7 ? mmYyyy.substring(3) + mmYyyy.substring(0, 2) : mmYyyy;
    }

    private static String ha(double v) {
        String s = String.format(PT_BR, "%,.1f", v);
        if (s.endsWith(",0")) s = s.substring(0, s.length() - 2);
        return s + " ha";
    }

    private static String culturaOuPadrao(String c) { return notBlank(c) ? c.trim() : "Sem cultura"; }
    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }
    private static String nn(String s) { return s == null ? "" : s; }

    /** Compara ignorando maiúsculas, espaços e acentos ("feijao" == "Feijão"). */
    static boolean mesmoTexto(String a, String b) {
        if (a == null || b == null) return false;
        return normalizar(a).equals(normalizar(b));
    }

    private static String normalizar(String s) {
        String n = Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }
}
