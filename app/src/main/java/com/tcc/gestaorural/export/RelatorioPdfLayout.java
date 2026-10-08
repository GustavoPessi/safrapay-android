package com.tcc.gestaorural.export;

/**
 * Layout do PDF do relatório por período (A4 retrato). PROTÓTIPO (30/09/2026):
 * cores e hierarquia seguem o Design System do app (verde #2E7D32, receita
 * verde, despesa vermelha, fundo #F3F7F2).
 *
 * Estrutura:
 *  1. Cabeçalho verde com SafraPay, título, período e data de geração
 *     (páginas seguintes: cabeçalho compacto).
 *  2. Três cartões: Receitas, Despesas, Saldo.
 *  3. Resumo mensal (tabela + barras comparativas).
 *  4. Despesas por categoria (valor, % e barra).
 *  5. Lançamentos do período (tabela que quebra página repetindo o cabeçalho).
 *  6. Rodapé com "Página X de Y".
 *
 * Uso: desenhar uma vez com Tela.SoMedida pra contar as páginas e depois de
 * verdade passando o total.
 */
public final class RelatorioPdfLayout {

    public static final float LARGURA = 595f;
    public static final float ALTURA = 842f;
    private static final float M = 40f;                 // margem lateral
    private static final float UTIL = LARGURA - 2 * M;  // 515
    private static final float LIMITE = ALTURA - 58f;   // fim da área de conteúdo (acima do rodapé)

    // Cores (ARGB)
    private static final int VERDE_900 = 0xFF1B5E20;
    private static final int VERDE_700 = 0xFF2E7D32;
    private static final int VERDE_300 = 0xFFA5D6A7;
    private static final int VERDE_100 = 0xFFE8F5E9;
    private static final int VERMELHO = 0xFFC62828;
    private static final int VERMELHO_CLARO = 0xFFEF9A9A;
    private static final int TEXTO = 0xFF1A241C;
    private static final int TEXTO_2 = 0xFF607063;
    private static final int FUNDO = 0xFFF3F7F2;
    private static final int ZEBRA = 0xFFF7FAF6;
    private static final int CONTORNO = 0xFFDCE3D6;
    private static final int BRANCO = 0xFFFFFFFF;

    private static final float ALT_CAB_TABELA = 20f;
    private static final float ALT_LINHA = 18f;

    private final RelatorioDados r;
    private final Tela t;
    private final int totalPaginas;
    private int pagina = 0;
    private float y;

    private RelatorioPdfLayout(RelatorioDados r, Tela t, int totalPaginas) {
        this.r = r;
        this.t = t;
        this.totalPaginas = totalPaginas;
    }

    /** Desenha o relatório e devolve quantas páginas ele ocupa. */
    public static int desenhar(RelatorioDados dados, Tela tela, int totalPaginas) {
        RelatorioPdfLayout l = new RelatorioPdfLayout(dados, tela, totalPaginas);
        l.executar();
        return l.pagina;
    }

    private void executar() {
        novaPagina();
        cartoes();
        resumoMensal();
        despesasPorCategoria();
        lancamentos();
        avisoGerencial();
        t.finalizarPagina();
    }

    // ------------------------------------------------------------------
    // Página, cabeçalho e rodapé
    // ------------------------------------------------------------------

    private void novaPagina() {
        if (pagina > 0) t.finalizarPagina();
        pagina++;
        t.iniciarPagina(pagina);
        if (pagina == 1) cabecalhoCompleto(); else cabecalhoCompacto();
        rodape();
    }

    private void cabecalhoCompleto() {
        float alt = 92f;
        t.retangulo(0, 0, LARGURA, alt, VERDE_700);
        t.retangulo(0, alt, LARGURA, 4f, VERDE_300);
        t.texto("SafraPay", M, 42, 24, true, BRANCO, Tela.ESQUERDA);
        t.texto("Relatório financeiro por período", M, 64, 11.5f, false, VERDE_100, Tela.ESQUERDA);

        float xd = LARGURA - M;
        t.texto("PERÍODO", xd, 34, 7.5f, true, VERDE_300, Tela.DIREITA);
        t.texto(r.periodoPorExtenso(), xd, 50, 12, true, BRANCO, Tela.DIREITA);
        t.texto("Gerado em " + nn(r.geradoEm), xd, 66, 8.5f, false, VERDE_100, Tela.DIREITA);

        y = alt + 4f + 26f;
        String produtor = nn(r.produtor);
        if (!produtor.isEmpty()) {
            t.texto("Produtor", M, y, 8, true, TEXTO_2, Tela.ESQUERDA);
            t.texto(produtor, M + t.larguraTexto("Produtor", 8, true) + 6, y, 10, false, TEXTO, Tela.ESQUERDA);
        }
        t.texto(r.lancamentos.size() + (r.lancamentos.size() == 1 ? " lançamento" : " lançamentos"),
                LARGURA - M, y, 9, false, TEXTO_2, Tela.DIREITA);
        y += 14f;
    }

    private void cabecalhoCompacto() {
        t.texto("SafraPay", M, 34, 12, true, VERDE_700, Tela.ESQUERDA);
        t.texto("Relatório financeiro por período", M + t.larguraTexto("SafraPay", 12, true) + 8, 34,
                9, false, TEXTO_2, Tela.ESQUERDA);
        t.texto(r.periodoPorExtenso(), LARGURA - M, 34, 9, false, TEXTO_2, Tela.DIREITA);
        t.linha(M, 44, LARGURA - M, 44, 0.8f, CONTORNO);
        y = 62f;
    }

    private void rodape() {
        float yl = ALTURA - 40f;
        t.linha(M, yl, LARGURA - M, yl, 0.8f, CONTORNO);
        t.texto("SafraPay • " + Formatos.AVISO_GERENCIAL_CURTO, M, yl + 16, 8, false, TEXTO_2, Tela.ESQUERDA);
        String pg = totalPaginas > 0 ? "Página " + pagina + " de " + totalPaginas : "Página " + pagina;
        t.texto(pg, LARGURA - M, yl + 16, 8, false, TEXTO_2, Tela.DIREITA);
    }

    /** Garante espaço vertical; se não couber, abre página nova. Devolve true se quebrou. */
    private boolean garantir(float altura) {
        if (y + altura <= LIMITE) return false;
        novaPagina();
        return true;
    }

    // ------------------------------------------------------------------
    // Cartões
    // ------------------------------------------------------------------

    private void cartoes() {
        float gap = 12f;
        float w = (UTIL - 2 * gap) / 3f;
        float h = 62f;
        cartao(M, w, h, "Receitas", Formatos.moeda(r.totalReceitas), VERDE_700);
        cartao(M + w + gap, w, h, "Despesas", Formatos.moeda(r.totalDespesas), VERMELHO);
        double s = r.saldo();
        cartao(M + 2 * (w + gap), w, h, "Saldo do período", Formatos.moeda(s), s >= 0 ? VERDE_700 : VERMELHO);
        y += h + 8f;
    }

    private void cartao(float x, float w, float h, String rotulo, String valor, int cor) {
        t.retangulo(x, y, w, h, FUNDO);
        t.retangulo(x, y, 4f, h, cor);
        t.texto(rotulo.toUpperCase(new java.util.Locale("pt", "BR")), x + 16, y + 22, 7.5f, true, TEXTO_2, Tela.ESQUERDA);
        float tam = ajustarTamanho(valor, w - 28, 16f, true);
        t.texto(valor, x + 16, y + 45, tam, true, cor, Tela.ESQUERDA);
    }

    private float ajustarTamanho(String s, float maxLargura, float tamanho, boolean negrito) {
        float tam = tamanho;
        while (tam > 9f && t.larguraTexto(s, tam, negrito) > maxLargura) tam -= 0.5f;
        return tam;
    }

    // ------------------------------------------------------------------
    // Seções
    // ------------------------------------------------------------------

    private void tituloSecao(String titulo, float espacoMinimoDepois) {
        garantir(34f + espacoMinimoDepois);
        y += 20f;
        t.texto(titulo, M, y, 12.5f, true, VERDE_900, Tela.ESQUERDA);
        y += 7f;
        t.linha(M, y, M + 28f, y, 2f, VERDE_700);
        y += 9f;
    }

    private void textoVazio(String s) {
        garantir(24f);
        y += 14f;
        t.texto(s, M, y, 9.5f, false, TEXTO_2, Tela.ESQUERDA);
        y += 6f;
    }

    // --- Resumo mensal -------------------------------------------------

    private static final float[] COL_MES = {95, 95, 95, 95, 135};
    private static final int[] AL_MES = {Tela.ESQUERDA, Tela.DIREITA, Tela.DIREITA, Tela.DIREITA, Tela.ESQUERDA};
    private static final String[] CAB_MES = {"Mês", "Receitas", "Despesas", "Saldo", "Receitas x despesas"};

    private void resumoMensal() {
        tituloSecao("Resumo mensal", ALT_CAB_TABELA + ALT_LINHA);
        if (r.meses.isEmpty()) {
            textoVazio("Sem lançamentos no período.");
            return;
        }
        double max = 0;
        for (RelatorioDados.Mes m : r.meses) max = Math.max(max, Math.max(m.receita, m.despesa));
        if (max <= 0) max = 1;

        cabecalhoTabela(CAB_MES, COL_MES, AL_MES);
        int i = 0;
        for (RelatorioDados.Mes m : r.meses) {
            if (garantir(ALT_LINHA)) cabecalhoTabela(CAB_MES, COL_MES, AL_MES);
            fundoLinha(i++);
            float[] xs = posicoes(COL_MES);
            celula(Formatos.mes(m.mes), xs, COL_MES, 0, AL_MES, false, TEXTO);
            celula(Formatos.moeda(m.receita), xs, COL_MES, 1, AL_MES, false, TEXTO);
            celula(Formatos.moeda(m.despesa), xs, COL_MES, 2, AL_MES, false, TEXTO);
            double s = m.saldo();
            celula(Formatos.moeda(s), xs, COL_MES, 3, AL_MES, true, s >= 0 ? VERDE_700 : VERMELHO);
            // barras: receita em cima, despesa embaixo
            float bx = xs[4] + 14, bw = COL_MES[4] - 20;
            t.retangulo(bx, y + 4.5f, (float) (bw * m.receita / max), 4f, VERDE_700);
            t.retangulo(bx, y + 10f, (float) (bw * m.despesa / max), 4f, VERMELHO_CLARO);
            y += ALT_LINHA;
        }
        // total (+ legenda logo abaixo)
        garantir(ALT_LINHA + 16);
        t.retangulo(M, y, UTIL, ALT_LINHA + 2, VERDE_100);
        float[] xs = posicoes(COL_MES);
        float yb = y + 13f;
        t.texto("Total", xs[0] + 8, yb, 9, true, VERDE_900, Tela.ESQUERDA);
        t.texto(Formatos.moeda(r.totalReceitas), xs[1] + COL_MES[1] - 8, yb, 9, true, TEXTO, Tela.DIREITA);
        t.texto(Formatos.moeda(r.totalDespesas), xs[2] + COL_MES[2] - 8, yb, 9, true, TEXTO, Tela.DIREITA);
        double s = r.saldo();
        t.texto(Formatos.moeda(s), xs[3] + COL_MES[3] - 8, yb, 9, true, s >= 0 ? VERDE_700 : VERMELHO, Tela.DIREITA);
        y += ALT_LINHA + 2;
        // legenda das barras
        y += 12f;
        float lx = M + UTIL - 150;
        t.retangulo(lx, y - 6, 10, 4, VERDE_700);
        t.texto("Receitas", lx + 14, y - 2, 7.5f, false, TEXTO_2, Tela.ESQUERDA);
        t.retangulo(lx + 70, y - 6, 10, 4, VERMELHO_CLARO);
        t.texto("Despesas", lx + 84, y - 2, 7.5f, false, TEXTO_2, Tela.ESQUERDA);
    }

    // --- Despesas por categoria ---------------------------------------

    private static final float[] COL_CAT = {190, 105, 70, 150};
    private static final int[] AL_CAT = {Tela.ESQUERDA, Tela.DIREITA, Tela.DIREITA, Tela.ESQUERDA};
    private static final String[] CAB_CAT = {"Categoria", "Total", "%", ""};

    private void despesasPorCategoria() {
        tituloSecao("Despesas por categoria", ALT_CAB_TABELA + ALT_LINHA);
        if (r.categorias.isEmpty()) {
            textoVazio("Sem despesas no período.");
            return;
        }
        double maior = r.categorias.get(0).total;
        if (maior <= 0) maior = 1;
        cabecalhoTabela(CAB_CAT, COL_CAT, AL_CAT);
        int i = 0;
        for (RelatorioDados.Categoria c : r.categorias) {
            if (garantir(ALT_LINHA)) cabecalhoTabela(CAB_CAT, COL_CAT, AL_CAT);
            fundoLinha(i++);
            float[] xs = posicoes(COL_CAT);
            double frac = r.totalDespesas > 0 ? c.total / r.totalDespesas : 0;
            celula(c.nome, xs, COL_CAT, 0, AL_CAT, false, TEXTO);
            celula(Formatos.moeda(c.total), xs, COL_CAT, 1, AL_CAT, false, TEXTO);
            celula(Formatos.percentual(frac), xs, COL_CAT, 2, AL_CAT, false, TEXTO_2);
            float bx = xs[3] + 14, bw = COL_CAT[3] - 20;
            t.retangulo(bx, y + 6.5f, bw, 5f, FUNDO);
            t.retangulo(bx, y + 6.5f, (float) (bw * c.total / maior), 5f, VERMELHO_CLARO);
            y += ALT_LINHA;
        }
    }

    // --- Lançamentos ---------------------------------------------------

    // Sem coluna "Tipo": receita/despesa aparece na faixa colorida à esquerda da
    // linha e no sinal/cor do valor (sobra largura pra descrição e valor).
    private static final float[] COL_LANC = {64, 163, 128, 72, 88};
    private static final int[] AL_LANC = {Tela.ESQUERDA, Tela.ESQUERDA, Tela.ESQUERDA, Tela.ESQUERDA, Tela.DIREITA};
    private static final String[] CAB_LANC = {"Data", "Descrição", "Cultura • Safra • Talhão", "Categoria", "Valor"};

    private void lancamentos() {
        tituloSecao("Lançamentos do período", ALT_CAB_TABELA + ALT_LINHA);
        if (r.lancamentos.isEmpty()) {
            textoVazio("Sem lançamentos no período.");
            return;
        }
        cabecalhoTabela(CAB_LANC, COL_LANC, AL_LANC);
        int i = 0;
        for (Lancamento l : r.lancamentos) {
            if (garantir(ALT_LINHA)) cabecalhoTabela(CAB_LANC, COL_LANC, AL_LANC);
            fundoLinha(i++);
            float[] xs = posicoes(COL_LANC);
            int cor = l.ehReceita() ? VERDE_700 : VERMELHO;
            t.retangulo(M, y + 2f, 2.5f, ALT_LINHA - 4f, l.ehReceita() ? VERDE_700 : VERMELHO_CLARO);
            celula(Formatos.dataBr(l.data), xs, COL_LANC, 0, AL_LANC, false, TEXTO);
            celula(l.descricao, xs, COL_LANC, 1, AL_LANC, false, TEXTO);
            celula(l.origem(), xs, COL_LANC, 2, AL_LANC, false, TEXTO_2);
            celula(l.ehReceita() ? "Receita" : l.categoria, xs, COL_LANC, 3, AL_LANC, false, TEXTO_2);
            celula((l.ehReceita() ? "+ " : "− ") + Formatos.moeda(l.valor), xs, COL_LANC, 4, AL_LANC, true, cor);
            y += ALT_LINHA;
        }
        t.linha(M, y, M + UTIL, y, 0.8f, CONTORNO);
        // legenda
        garantir(16f);
        y += 13f;
        t.retangulo(M, y - 7, 2.5f, 8, VERDE_700);
        t.texto("Receita", M + 7, y, 7.5f, false, TEXTO_2, Tela.ESQUERDA);
        t.retangulo(M + 50, y - 7, 2.5f, 8, VERMELHO_CLARO);
        t.texto("Despesa", M + 57, y, 7.5f, false, TEXTO_2, Tela.ESQUERDA);
    }

    // --- Aviso de relatório gerencial (08/10/2026) ----------------------

    /** Caixa no fim do relatório com o aviso completo (o rodapé de toda página traz a versão curta). */
    private void avisoGerencial() {
        float tam = 8f, entre = 11f, pad = 10f;
        java.util.List<String> linhas = quebrar(Formatos.AVISO_GERENCIAL, UTIL - 2 * pad - 6, tam);
        float alt = pad * 2 + 12f + linhas.size() * entre;
        garantir(alt + 18f);
        y += 18f;
        t.retangulo(M, y, UTIL, alt, FUNDO);
        t.retangulo(M, y, 3f, alt, TEXTO_2);
        float yt = y + pad + 8f;
        t.texto("Aviso", M + pad + 6, yt, 8.5f, true, TEXTO, Tela.ESQUERDA);
        for (String l : linhas) {
            yt += entre;
            t.texto(l, M + pad + 6, yt, tam, false, TEXTO_2, Tela.ESQUERDA);
        }
        y += alt;
    }

    /** Quebra o texto em linhas que cabem na largura (por palavra). */
    private java.util.List<String> quebrar(String texto, float max, float tam) {
        java.util.List<String> linhas = new java.util.ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split(" ")) {
            String teste = atual.length() == 0 ? palavra : atual + " " + palavra;
            if (t.larguraTexto(teste, tam, false) <= max || atual.length() == 0) {
                atual.setLength(0);
                atual.append(teste);
            } else {
                linhas.add(atual.toString());
                atual.setLength(0);
                atual.append(palavra);
            }
        }
        if (atual.length() > 0) linhas.add(atual.toString());
        return linhas;
    }

    // ------------------------------------------------------------------
    // Tabela genérica
    // ------------------------------------------------------------------

    private void cabecalhoTabela(String[] titulos, float[] cols, int[] al) {
        garantir(ALT_CAB_TABELA + ALT_LINHA);
        t.retangulo(M, y, UTIL, ALT_CAB_TABELA, VERDE_700);
        float[] xs = posicoes(cols);
        for (int i = 0; i < titulos.length; i++) {
            if (titulos[i].isEmpty()) continue;
            float x = al[i] == Tela.DIREITA ? xs[i] + cols[i] - 8 : xs[i] + 8;
            t.texto(cortar(titulos[i], cols[i] - 12, 8.5f, true), x, y + 13.5f, 8.5f, true, BRANCO, al[i]);
        }
        y += ALT_CAB_TABELA;
    }

    private void fundoLinha(int indice) {
        if (indice % 2 == 1) t.retangulo(M, y, UTIL, ALT_LINHA, ZEBRA);
    }

    private void celula(String s, float[] xs, float[] cols, int i, int[] al, boolean negrito, int cor) {
        float tam = 8.5f;
        String txt = cortar(nn(s), cols[i] - 12, tam, negrito);
        float x = al[i] == Tela.DIREITA ? xs[i] + cols[i] - 8 : xs[i] + 8;
        t.texto(txt, x, y + 12.5f, tam, negrito, cor, al[i]);
    }

    private static float[] posicoes(float[] cols) {
        float[] xs = new float[cols.length];
        float x = M;
        for (int i = 0; i < cols.length; i++) {
            xs[i] = x;
            x += cols[i];
        }
        return xs;
    }

    /** Corta o texto com "…" pra caber na largura. */
    private String cortar(String s, float max, float tam, boolean negrito) {
        if (t.larguraTexto(s, tam, negrito) <= max) return s;
        String ret = "…";
        int fim = s.length();
        while (fim > 0 && t.larguraTexto(s.substring(0, fim).trim() + ret, tam, negrito) > max) fim--;
        return fim <= 0 ? ret : s.substring(0, fim).trim() + ret;
    }

    private static String nn(String s) { return s == null ? "" : s; }
}
