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
import com.tcc.gestaorural.data.local.entity.Anexo;
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
        private String titulo;
        private String observacao;
        private final List<String> linhas = new ArrayList<>();
        private String total;
        /** Só na consulta COMPROVANTE: os cards de foto a mostrar (null nas demais). */
        private List<Comprovante> comprovantes;

        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getObservacao() { return observacao; }
        public void setObservacao(String observacao) { this.observacao = observacao; }
        public List<String> getLinhas() { return linhas; }
        public String getTotal() { return total; }
        public void setTotal(String total) { this.total = total; }
        public List<Comprovante> getComprovantes() { return comprovantes; }
        public void setComprovantes(List<Comprovante> comprovantes) { this.comprovantes = comprovantes; }
    }

    /** Comprovante achado na consulta COMPROVANTE (caminho null = lançamento sem foto). */
    public static class Comprovante {
        private final String tipo;          // DESPESA | RECEITA
        private final long lancamentoId;
        private final String caminho;
        private final String legenda;

        public Comprovante(String tipo, long lancamentoId, String caminho, String legenda) {
            this.tipo = tipo;
            this.lancamentoId = lancamentoId;
            this.caminho = caminho;
            this.legenda = legenda;
        }

        public String getTipo() { return tipo; }
        public long getLancamentoId() { return lancamentoId; }
        public String getCaminho() { return caminho; }
        public String getLegenda() { return legenda; }
        public boolean temFoto() { return caminho != null; }
    }

    public interface Callback { void onResultado(Resultado r); }

    private static final String SEM_INICIO = "0000-01-01";
    private static final String SEM_FIM = "9999-12-31";
    private static final int MAX_ITENS = 15;
    private static final int MAX_COMPROVANTES = 5;
    private static final Locale PT_BR = new Locale("pt", "BR");

    private final DespesaDao despesaDao;
    private final ReceitaDao receitaDao;
    private final SafraDao safraDao;
    private final PropriedadeDao propriedadeDao;
    private final TalhaoDao talhaoDao;
    private final MetaFinanceiraDao metaDao;
    private final MaquinaDao maquinaDao;
    private final AnotacaoMaquinaDao anotacaoDao;
    private final AnexoRepository anexoRepo;
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
        anexoRepo = new AnexoRepository(context);
    }

    public void executar(long usuarioId, ChatMensagemResponseDto.Consulta c, Callback cb) {
        AppDatabase.IO.execute(() -> {
            Resultado r;
            try {
                r = calcular(usuarioId, c);
            } catch (Exception e) {
                r = new Resultado();
                r.setTitulo("Não consegui calcular");
                r.getLinhas().add("Tive um problema ao buscar esses dados no aparelho. Tente perguntar de outro jeito.");
            }
            final Resultado fr = r;
            main.post(() -> cb.onResultado(fr));
        });
    }

    // ------------------------------------------------------------------

    Resultado calcular(long u, ChatMensagemResponseDto.Consulta c) {
        String tipo = c.getTipo() == null ? "" : c.getTipo().trim().toUpperCase(Locale.ROOT);
        switch (tipo) {
            case "GASTOS":      return lancamentos(u, c, true);
            case "RECEITAS":    return lancamentos(u, c, false);
            case "SALDO":       return saldo(u, c);
            case "LANCAMENTOS": return ultimos(u, c);
            case "SAFRAS":      return safras(u, c);
            case "AREA":        return area(u, c);
            case "METAS":       return metas(u, c);
            case "MAQUINAS":    return maquinas(u, c);
            case "COMPROVANTE": return comprovantes(u, c);
            default:
                Resultado r = new Resultado();
                r.setTitulo("Consulta não reconhecida");
                r.getLinhas().add("Ainda não sei responder esse tipo de pergunta.");
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
        r.setTitulo((despesa ? "Gastos" : "Receitas") + descreverFiltros(u, c));
        r.setObservacao(p.descricao);
        double total = 0;
        for (LancamentoLinha l : lista) total += l.getValor();

        if (lista.isEmpty()) {
            r.getLinhas().add(despesa ? "Nenhum gasto encontrado." : "Nenhuma receita encontrada.");
            r.setTotal("Total: " + MoneyUtils.formatar(0));
            return r;
        }
        if (notBlank(c.getAgrupar())) {
            agrupar(r, lista, c.getAgrupar());
        } else {
            for (int i = 0; i < lista.size() && i < MAX_ITENS; i++) r.getLinhas().add(linhaItem(lista.get(i), false));
            if (lista.size() > MAX_ITENS) r.getLinhas().add("… e mais " + (lista.size() - MAX_ITENS) + " lançamento(s)");
        }
        r.setTotal("Total: " + MoneyUtils.formatar(total) + "  (" + lista.size() + " lançamento" + (lista.size() == 1 ? "" : "s") + ")");
        return r;
    }

    // --- SALDO --------------------------------------------------------

    private Resultado saldo(long u, ChatMensagemResponseDto.Consulta c) {
        Periodo p = periodo(c, true);
        double rec = soma(filtrar(receitaDao.listarLinhas(u, p.ini, p.fim), c));
        double des = soma(filtrar(despesaDao.listarLinhas(u, p.ini, p.fim), c));
        Resultado r = new Resultado();
        r.setTitulo("Resultado" + descreverFiltros(u, c));
        r.setObservacao(p.descricao);
        r.getLinhas().add("Receitas: " + MoneyUtils.formatar(rec));
        r.getLinhas().add("Despesas: " + MoneyUtils.formatar(des));
        double s = rec - des;
        r.setTotal((s >= 0 ? "Lucro: " : "Prejuízo: ") + MoneyUtils.formatar(Math.abs(s)));
        return r;
    }

    // --- LANCAMENTOS (últimos) ----------------------------------------

    private Resultado ultimos(long u, ChatMensagemResponseDto.Consulta c) {
        Periodo p = periodo(c, true);
        List<LancamentoLinha> todos = new ArrayList<>(filtrar(despesaDao.listarLinhas(u, p.ini, p.fim), c));
        todos.addAll(filtrar(receitaDao.listarLinhas(u, p.ini, p.fim), c));
        todos.sort((a, b) -> nn(b.getData()).compareTo(nn(a.getData())));

        Resultado r = new Resultado();
        r.setTitulo("Lançamentos" + descreverFiltros(u, c));
        r.setObservacao(p.descricao);
        if (todos.isEmpty()) { r.getLinhas().add("Nenhum lançamento encontrado."); return r; }
        int limite = Math.min(20, todos.size());
        for (int i = 0; i < limite; i++) r.getLinhas().add(linhaItem(todos.get(i), true));
        if (todos.size() > limite) r.getLinhas().add("… e mais " + (todos.size() - limite) + " lançamento(s)");
        r.setTotal(todos.size() + " lançamento" + (todos.size() == 1 ? "" : "s"));
        return r;
    }

    // --- COMPROVANTE (v3): mostrar a foto da nota/recibo -----------------

    private Resultado comprovantes(long u, ChatMensagemResponseDto.Consulta c) {
        // Sem datas = sem filtro de data ("a nota da ureia" procura em tudo).
        Periodo p = periodo(c, false);
        List<LancamentoLinha> todos = new ArrayList<>(filtrar(despesaDao.listarLinhas(u, p.ini, p.fim), c));
        todos.addAll(filtrar(receitaDao.listarLinhas(u, p.ini, p.fim), c));
        todos.sort((a, b) -> nn(b.getData()).compareTo(nn(a.getData())));

        Resultado r = new Resultado();
        r.setTitulo("Comprovantes");
        List<Comprovante> achados = new ArrayList<>();
        r.setComprovantes(achados);

        int comFoto = 0;
        for (LancamentoLinha l : todos) {
            Anexo a = anexoRepo.buscarSync(l.getTipo(), l.getUuid());
            if (a == null) continue;
            comFoto++;
            if (achados.size() < MAX_COMPROVANTES) {
                achados.add(new Comprovante(l.getTipo(), l.getId(), a.getCaminho(), legenda(l)));
            }
        }

        if (comFoto == 0) {
            if (todos.isEmpty()) {
                r.getLinhas().add("Não encontrei esse lançamento.");
            } else {
                LancamentoLinha l = todos.get(0);
                r.getLinhas().add("Achei o lançamento (" + legendaCurta(l) + "), mas ele não tem comprovante anexado.");
                achados.add(new Comprovante(l.getTipo(), l.getId(), null, legenda(l)));
            }
        } else if (comFoto > MAX_COMPROVANTES) {
            r.getLinhas().add("Encontrei " + comFoto + " comprovantes. Estes são os " + MAX_COMPROVANTES
                    + " mais recentes — diga a data ou o valor pra eu achar o certo.");
        }
        return r;
    }

    private static String legenda(LancamentoLinha l) {
        return (notBlank(l.getDescricao()) ? l.getDescricao().trim() : "Lançamento")
                + " • " + MoneyUtils.formatar(l.getValor())
                + " • " + (l.getData() != null ? DateUtils.isoParaBr(l.getData()) : "sem data");
    }

    private static String legendaCurta(LancamentoLinha l) {
        String data = l.getData() != null && l.getData().length() >= 10
                ? l.getData().substring(8, 10) + "/" + l.getData().substring(5, 7) : "sem data";
        return (notBlank(l.getDescricao()) ? l.getDescricao().trim() : "Lançamento")
                + ", " + MoneyUtils.formatar(l.getValor()) + ", " + data;
    }

    // --- SAFRAS -------------------------------------------------------

    private Resultado safras(long u, ChatMensagemResponseDto.Consulta c) {
        String situacao = c.getSituacao() == null ? "ATIVAS" : c.getSituacao().trim().toUpperCase(Locale.ROOT);
        List<SafraContexto> todas = safraDao.listarContexto(u);
        boolean variasPropriedades = contarPropriedades(todas) > 1;

        List<SafraContexto> lista = new ArrayList<>();
        for (SafraContexto s : todas) {
            if ("ATIVAS".equals(situacao) && !s.ativa()) continue;
            if ("ENCERRADAS".equals(situacao) && s.ativa()) continue;
            if (c.getSafraUuid() != null && !c.getSafraUuid().equals(s.getUuid())) continue;
            if (c.getTalhaoUuid() != null && !c.getTalhaoUuid().equals(s.getTalhaoUuid())) continue;
            if (c.getPropriedadeUuid() != null && !c.getPropriedadeUuid().equals(s.getPropriedadeUuid())) continue;
            if (notBlank(c.getCultura()) && !mesmoTexto(c.getCultura(), s.getCultura())) continue;
            lista.add(s);
        }

        Resultado r = new Resultado();
        r.setTitulo(("ENCERRADAS".equals(situacao) ? "Safras encerradas"
                : "TODAS".equals(situacao) ? "Todas as safras" : "Safras em aberto")
                + (notBlank(c.getCultura()) ? " • " + c.getCultura() : ""));
        if (lista.isEmpty()) {
            r.getLinhas().add("ENCERRADAS".equals(situacao) ? "Nenhuma safra encerrada."
                    : "TODAS".equals(situacao) ? "Nenhuma safra cadastrada."
                    : "Nenhuma safra em aberto no momento. Para abrir uma, use Início → + Nova safra.");
            return r;
        }
        for (SafraContexto s : lista) {
            StringBuilder sb = new StringBuilder();
            sb.append("🌱 ").append(culturaOuPadrao(s.getCultura())).append(" • ").append(nn(s.getAnoSafra()));
            if (notBlank(s.getTalhaoNome())) sb.append(" — Talhão ").append(s.getTalhaoNome());
            if (variasPropriedades && notBlank(s.getPropriedadeNome())) sb.append(" (").append(s.getPropriedadeNome()).append(")");
            sb.append("\n     Início: ").append(notBlank(s.getDataInicio()) ? s.getDataInicio() : "não informado");
            sb.append("  •  Fim: ").append(s.ativa() ? "em andamento" : s.getDataFim());
            r.getLinhas().add(sb.toString());
        }
        r.setTotal(lista.size() + " safra" + (lista.size() == 1 ? "" : "s"));
        return r;
    }

    // --- AREA ---------------------------------------------------------

    private Resultado area(long u, ChatMensagemResponseDto.Consulta c) {
        List<Propriedade> props = propriedadeDao.listarPorUsuarioSync(u);
        List<Talhao> tals = talhaoDao.listarPorUsuarioSync(u);
        Map<Long, Propriedade> porId = new HashMap<>();
        for (Propriedade p : props) porId.put(p.id, p);

        Resultado r = new Resultado();
        if (props.isEmpty()) { r.setTitulo("Área"); r.getLinhas().add("Nenhuma propriedade cadastrada."); return r; }

        if (notBlank(c.getCultura()) || c.getTalhaoUuid() != null) {
            double soma = 0; int semArea = 0;
            r.setTitulo(notBlank(c.getCultura()) ? "Área de " + c.getCultura() : "Área do talhão");
            for (Talhao t : tals) {
                if (c.getTalhaoUuid() != null && !c.getTalhaoUuid().equals(t.uuid)) continue;
                if (notBlank(c.getCultura()) && !mesmoTexto(c.getCultura(), t.cultura)) continue;
                Propriedade p = porId.get(t.propriedadeId);
                if (c.getPropriedadeUuid() != null && (p == null || !c.getPropriedadeUuid().equals(p.uuid))) continue;
                if (t.areaHa == null) semArea++; else soma += t.areaHa;
                r.getLinhas().add("Talhão " + t.nome + (p != null && props.size() > 1 ? " (" + p.nome + ")" : "")
                        + ": " + (t.areaHa == null ? "área não informada" : ha(t.areaHa)));
            }
            if (r.getLinhas().isEmpty()) r.getLinhas().add("Nenhum talhão encontrado com esse filtro.");
            r.setTotal("Total: " + ha(soma) + (semArea > 0 ? "  (" + semArea + " sem área informada)" : ""));
            return r;
        }

        r.setTitulo("Área das propriedades");
        double total = 0;
        Map<String, Double> porCultura = new LinkedHashMap<>();
        for (Propriedade p : props) {
            if (c.getPropriedadeUuid() != null && !c.getPropriedadeUuid().equals(p.uuid)) continue;
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
            r.getLinhas().add("🏡 " + p.nome + ": " + (p.areaTotal != null ? ha(p.areaTotal) : "área total não informada")
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
            r.getLinhas().add(sb.toString());
        }
        r.setTotal("Área total: " + ha(total));
        return r;
    }

    // --- METAS --------------------------------------------------------

    private Resultado metas(long u, ChatMensagemResponseDto.Consulta c) {
        List<MetaContexto> metas = metaDao.listarContextoSync(u);
        Map<String, Double> saldoPorSafra = new HashMap<>();
        for (LancamentoLinha l : receitaDao.listarLinhas(u, SEM_INICIO, SEM_FIM)) somar(saldoPorSafra, l.getSafraUuid(), l.getValor());
        for (LancamentoLinha l : despesaDao.listarLinhas(u, SEM_INICIO, SEM_FIM)) somar(saldoPorSafra, l.getSafraUuid(), -l.getValor());

        Resultado r = new Resultado();
        r.setTitulo("Metas" + (notBlank(c.getCultura()) ? " • " + c.getCultura() : ""));
        int n = 0;
        for (MetaContexto m : metas) {
            if (c.getSafraUuid() != null && !c.getSafraUuid().equals(m.getSafraUuid())) continue;
            if (notBlank(c.getCultura()) && !mesmoTexto(c.getCultura(), m.getCultura())) continue;
            double atual = Math.max(0, saldoPorSafra.containsKey(m.getSafraUuid()) ? saldoPorSafra.get(m.getSafraUuid()) : 0);
            int pct = m.getValorAlvo() > 0 ? (int) Math.min(100, Math.round(atual / m.getValorAlvo() * 100)) : 0;
            StringBuilder sb = new StringBuilder("🎯 ").append(m.getDescricao())
                    .append(" (").append(culturaOuPadrao(m.getCultura())).append(" ").append(nn(m.getAnoSafra())).append(")")
                    .append("\n     ").append(MoneyUtils.formatar(atual)).append(" de ").append(MoneyUtils.formatar(m.getValorAlvo()))
                    .append(" — ").append(pct).append("%");
            if (notBlank(m.getPrazo())) sb.append(" • prazo ").append(DateUtils.isoParaBr(m.getPrazo()));
            r.getLinhas().add(sb.toString());
            n++;
        }
        if (n == 0) r.getLinhas().add("Nenhuma meta encontrada. Você pode criar uma na aba Metas.");
        else r.setTotal(n + " meta" + (n == 1 ? "" : "s") + "  (progresso = receitas − despesas da safra)");
        return r;
    }

    // --- MAQUINAS -----------------------------------------------------

    private Resultado maquinas(long u, ChatMensagemResponseDto.Consulta c) {
        List<Maquina> maqs = maquinaDao.listarPorUsuarioSync(u);
        Periodo p = periodo(c, false);
        Map<String, Double> gastoPorMaquina = new HashMap<>();
        for (LancamentoLinha l : despesaDao.listarLinhas(u, p.ini, p.fim)) {
            if (l.getMaquinaUuid() != null) somar(gastoPorMaquina, l.getMaquinaUuid(), l.getValor());
        }
        Map<Long, List<AnotacaoMaquina>> pendentes = new HashMap<>();
        for (AnotacaoMaquina a : anotacaoDao.listarPendentesSync(u)) {
            if (!pendentes.containsKey(a.maquinaId)) pendentes.put(a.maquinaId, new ArrayList<>());
            pendentes.get(a.maquinaId).add(a);
        }

        Resultado r = new Resultado();
        r.setTitulo("Máquinas");
        r.setObservacao(p.descricao);
        if (maqs.isEmpty()) { r.getLinhas().add("Nenhuma máquina cadastrada."); return r; }
        double total = 0;
        for (Maquina m : maqs) {
            if (c.getMaquinaUuid() != null && !c.getMaquinaUuid().equals(m.uuid)) continue;
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
            r.getLinhas().add(sb.toString());
        }
        if (r.getLinhas().isEmpty()) r.getLinhas().add("Máquina não encontrada.");
        r.setTotal("Total com máquinas: " + MoneyUtils.formatar(total));
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
        String ini = notBlank(c.getDataInicio()) ? c.getDataInicio().trim() : null;
        String fim = notBlank(c.getDataFim()) ? c.getDataFim().trim() : null;
        if (ini == null && fim == null) {
            boolean temEscopo = c.getSafraUuid() != null || c.getTalhaoUuid() != null || notBlank(c.getCultura());
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
            if (c.getSafraUuid() != null && !c.getSafraUuid().equals(l.getSafraUuid())) continue;
            if (c.getTalhaoUuid() != null && !c.getTalhaoUuid().equals(l.getTalhaoUuid())) continue;
            if (c.getPropriedadeUuid() != null && !c.getPropriedadeUuid().equals(l.getPropriedadeUuid())) continue;
            if (notBlank(c.getCultura()) && !mesmoTexto(c.getCultura(), l.getCultura())) continue;
            if (notBlank(c.getCategoria()) && !mesmoTexto(c.getCategoria(), l.getCategoria())) continue;
            if (c.getMaquinaUuid() != null && !c.getMaquinaUuid().equals(l.getMaquinaUuid())) continue;
            if (notBlank(c.getTexto()) && !contemTexto(l.getDescricao(), c.getTexto())) continue;
            if (c.getValor() != null && c.getValor() > 0 && Math.abs(l.getValor() - c.getValor()) > 0.01) continue;
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
                case "CATEGORIA": chave = notBlank(l.getCategoria()) ? l.getCategoria() : "Sem categoria"; break;
                case "SAFRA":     chave = culturaOuPadrao(l.getCultura()) + " " + nn(l.getAnoSafra())
                                          + (notBlank(l.getTalhaoNome()) ? " — Talhão " + l.getTalhaoNome() : ""); break;
                case "MES":       chave = l.getData() != null && l.getData().length() >= 7
                                          ? l.getData().substring(5, 7) + "/" + l.getData().substring(0, 4) : "Sem data"; break;
                case "MAQUINA":   chave = notBlank(l.getMaquinaNome()) ? l.getMaquinaNome() : "Sem máquina"; break;
                default:          chave = "Total";
            }
            somar(grupos, chave, l.getValor());
        }
        List<Map.Entry<String, Double>> ordenado = new ArrayList<>(grupos.entrySet());
        if (!"MES".equals(modo)) ordenado.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        else ordenado.sort((a, b) -> chaveMes(a.getKey()).compareTo(chaveMes(b.getKey())));
        for (Map.Entry<String, Double> e : ordenado) r.getLinhas().add(e.getKey() + " — " + MoneyUtils.formatar(e.getValue()));
    }

    private String descreverFiltros(long u, ChatMensagemResponseDto.Consulta c) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(c.getCategoria())) sb.append(" • ").append(c.getCategoria());
        if (notBlank(c.getCultura())) sb.append(" • ").append(c.getCultura());
        if (c.getSafraUuid() != null) {
            for (SafraContexto s : safraDao.listarContexto(u)) {
                if (c.getSafraUuid().equals(s.getUuid())) { sb.append(" • ").append(culturaOuPadrao(s.getCultura())).append(" ").append(nn(s.getAnoSafra())); break; }
            }
        }
        if (c.getMaquinaUuid() != null) {
            Maquina m = maquinaDao.buscarPorUuid(c.getMaquinaUuid());
            if (m != null) sb.append(" • ").append(m.nome);
        }
        if (c.getTalhaoUuid() != null) {
            Talhao t = talhaoDao.buscarPorUuid(c.getTalhaoUuid());
            if (t != null) sb.append(" • Talhão ").append(t.nome);
        }
        if (c.getPropriedadeUuid() != null) {
            Propriedade p = propriedadeDao.buscarPorUuid(c.getPropriedadeUuid());
            if (p != null) sb.append(" • ").append(p.nome);
        }
        return sb.toString();
    }

    private static String linhaItem(LancamentoLinha l, boolean comTipo) {
        StringBuilder sb = new StringBuilder();
        sb.append(l.getData() != null && l.getData().length() >= 10 ? l.getData().substring(8, 10) + "/" + l.getData().substring(5, 7) : "--/--");
        if (comTipo) sb.append(" • ").append("DESPESA".equals(l.getTipo()) ? "Despesa" : "Receita");
        sb.append(" • ").append(notBlank(l.getDescricao()) ? l.getDescricao() : "(sem descrição)");
        if (notBlank(l.getCategoria())) sb.append(" (").append(l.getCategoria()).append(")");
        sb.append(" — ").append(MoneyUtils.formatar(l.getValor()));
        return sb.toString();
    }

    private static int contarPropriedades(List<SafraContexto> safras) {
        java.util.Set<String> s = new java.util.HashSet<>();
        for (SafraContexto x : safras) if (x.getPropriedadeUuid() != null) s.add(x.getPropriedadeUuid());
        return s.size();
    }

    private static double soma(List<LancamentoLinha> l) { double t = 0; for (LancamentoLinha x : l) t += x.getValor(); return t; }

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

    /** "diesel" acha "Óleo Diesel S10" (sem diferenciar maiúsculas/acentos). */
    static boolean contemTexto(String onde, String procurado) {
        if (onde == null || procurado == null) return false;
        return normalizar(onde).contains(normalizar(procurado));
    }

    private static String normalizar(String s) {
        String n = Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }
}
