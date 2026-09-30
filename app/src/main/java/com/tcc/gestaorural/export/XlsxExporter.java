package com.tcc.gestaorural.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Gera um .xlsx de verdade (Office Open XML) sem biblioteca externa: o arquivo
 * é só um .zip com alguns XMLs. Evita o Apache POI, que é pesado demais pro
 * Android. Duas abas:
 *  - "Resumo": período, produtor, indicadores (com fórmulas que somam a aba de
 *    lançamentos), tabela mês a mês e despesas por categoria.
 *  - "Lançamentos": uma linha por receita/despesa, com filtro e cabeçalho fixo.
 * Datas vão como data de verdade e valores como número com formato R$, então
 * dá pra somar, filtrar e fazer tabela dinâmica direto no Excel.
 */
public final class XlsxExporter {
    private XlsxExporter() {}

    // Índices de estilo (posição no <cellXfs> de STYLES).
    private static final int S_PADRAO = 0;
    private static final int S_NEGRITO = 1;
    private static final int S_CABECALHO = 2;
    private static final int S_MOEDA = 3;
    private static final int S_DATA = 4;
    private static final int S_TITULO = 5;
    private static final int S_TOTAL = 6;
    private static final int S_MOEDA_TOTAL = 7;
    private static final int S_PERCENTUAL = 8;
    private static final int S_CINZA = 9;
    private static final int S_RECEITA = 10;
    private static final int S_DESPESA = 11;

    private static final String ABA_LANC = "Lançamentos";
    private static final String REF_LANC = "'" + ABA_LANC + "'";

    public static void escrever(RelatorioDados r, OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        int ultimaLinhaLanc = r.lancamentos.size() + 1;
        adicionar(zip, "[Content_Types].xml", CONTENT_TYPES);
        adicionar(zip, "_rels/.rels", RELS);
        adicionar(zip, "xl/workbook.xml", workbook(ultimaLinhaLanc));
        adicionar(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
        adicionar(zip, "xl/styles.xml", STYLES);
        adicionar(zip, "xl/worksheets/sheet1.xml", abaResumo(r));
        adicionar(zip, "xl/worksheets/sheet2.xml", abaLancamentos(r));
        zip.finish();
        zip.flush();
    }

    // ------------------------------------------------------------------
    // Aba "Resumo"
    // ------------------------------------------------------------------

    private static String abaResumo(RelatorioDados r) {
        Aba a = new Aba();
        a.linha(1).texto(0, "SafraPay — Relatório financeiro por período", S_TITULO);
        a.linha(2).texto(0, "Período", S_NEGRITO).texto(1, r.periodoPorExtenso(), S_PADRAO);
        a.linha(3).texto(0, "Produtor", S_NEGRITO).texto(1, vazioSeNulo(r.produtor), S_PADRAO);
        a.linha(4).texto(0, "Gerado em", S_NEGRITO).texto(1, vazioSeNulo(r.geradoEm), S_PADRAO);

        // Indicadores: somam a aba de lançamentos (se o usuário editar lá, o resumo acompanha).
        a.linha(6).texto(0, "Indicador", S_CABECALHO).texto(1, "Valor", S_CABECALHO);
        a.linha(7).texto(0, "Receitas", S_PADRAO)
                .formula(1, "SUMIF(" + REF_LANC + "!B:B,\"Receita\"," + REF_LANC + "!D:D)", r.totalReceitas, S_MOEDA);
        a.linha(8).texto(0, "Despesas", S_PADRAO)
                .formula(1, "SUMIF(" + REF_LANC + "!B:B,\"Despesa\"," + REF_LANC + "!D:D)", r.totalDespesas, S_MOEDA);
        a.linha(9).texto(0, "Saldo", S_TOTAL).formula(1, "B7-B8", r.saldo(), S_MOEDA_TOTAL);
        a.linha(10).texto(0, "Lançamentos no período", S_PADRAO).numero(1, r.lancamentos.size(), S_PADRAO);

        // Resumo mês a mês
        int l = 12;
        a.linha(l).texto(0, "Mês", S_CABECALHO).texto(1, "Receitas", S_CABECALHO)
                .texto(2, "Despesas", S_CABECALHO).texto(3, "Saldo", S_CABECALHO);
        l++;
        if (r.meses.isEmpty()) {
            a.linha(l++).texto(0, "Sem lançamentos no período.", S_CINZA);
        } else {
            int primeira = l;
            for (RelatorioDados.Mes m : r.meses) {
                a.linha(l).texto(0, Formatos.mes(m.mes), S_PADRAO)
                        .numero(1, m.receita, S_MOEDA)
                        .numero(2, m.despesa, S_MOEDA)
                        .formula(3, "B" + l + "-C" + l, m.saldo(), S_MOEDA);
                l++;
            }
            int ultima = l - 1;
            a.linha(l).texto(0, "Total", S_TOTAL)
                    .formula(1, "SUM(B" + primeira + ":B" + ultima + ")", r.totalReceitas, S_MOEDA_TOTAL)
                    .formula(2, "SUM(C" + primeira + ":C" + ultima + ")", r.totalDespesas, S_MOEDA_TOTAL)
                    .formula(3, "B" + l + "-C" + l, r.saldo(), S_MOEDA_TOTAL);
            l++;
        }

        // Despesas por categoria
        l++;
        a.linha(l).texto(0, "Despesas por categoria", S_CABECALHO).texto(1, "Total", S_CABECALHO)
                .texto(2, "% das despesas", S_CABECALHO);
        l++;
        if (r.categorias.isEmpty()) {
            a.linha(l).texto(0, "Sem despesas no período.", S_CINZA);
        } else {
            int primeira = l;
            int ultima = l + r.categorias.size() - 1;
            for (RelatorioDados.Categoria c : r.categorias) {
                double frac = r.totalDespesas > 0 ? c.total / r.totalDespesas : 0;
                a.linha(l).texto(0, c.nome, S_PADRAO)
                        .numero(1, c.total, S_MOEDA)
                        .formula(2, "IF(SUM($B$" + primeira + ":$B$" + ultima + ")=0,0,B" + l
                                + "/SUM($B$" + primeira + ":$B$" + ultima + "))", frac, S_PERCENTUAL);
                l++;
            }
        }

        return a.xml(null, "<col min=\"1\" max=\"1\" width=\"30\" customWidth=\"1\"/>"
                + "<col min=\"2\" max=\"4\" width=\"18\" customWidth=\"1\"/>", null);
    }

    // ------------------------------------------------------------------
    // Aba "Lançamentos"
    // ------------------------------------------------------------------

    private static String abaLancamentos(RelatorioDados r) {
        Aba a = new Aba();
        Linha cab = a.linha(1);
        for (int i = 0; i < CsvExporter.CABECALHO.length; i++) cab.texto(i, CsvExporter.CABECALHO[i], S_CABECALHO);

        int l = 2;
        for (Lancamento x : r.lancamentos) {
            Linha ln = a.linha(l++);
            long serial = Formatos.serialExcel(x.data);
            if (serial > 0) ln.numero(0, serial, S_DATA); else ln.texto(0, vazioSeNulo(x.data), S_PADRAO);
            ln.texto(1, x.tipoPorExtenso(), x.ehReceita() ? S_RECEITA : S_DESPESA)
              .texto(2, vazioSeNulo(x.descricao), S_PADRAO)
              .numero(3, x.valor, S_MOEDA)
              .texto(4, vazioSeNulo(x.propriedade), S_PADRAO)
              .texto(5, vazioSeNulo(x.talhao), S_PADRAO)
              .texto(6, vazioSeNulo(x.cultura), S_PADRAO)
              .texto(7, vazioSeNulo(x.safra), S_PADRAO)
              .texto(8, vazioSeNulo(x.categoria), S_PADRAO)
              .texto(9, vazioSeNulo(x.maquina), S_PADRAO);
        }

        String vistas = "<sheetViews><sheetView workbookViewId=\"0\">"
                + "<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>"
                + "<selection pane=\"bottomLeft\" activeCell=\"A2\" sqref=\"A2\"/>"
                + "</sheetView></sheetViews>";
        String colunas = largura(1, 12) + largura(2, 10) + largura(3, 40) + largura(4, 15)
                + largura(5, 24) + largura(6, 18) + largura(7, 14) + largura(8, 12)
                + largura(9, 24) + largura(10, 20);
        String filtro = "<autoFilter ref=\"A1:J" + (r.lancamentos.size() + 1) + "\"/>";
        return a.xml(vistas, colunas, filtro);
    }

    private static String largura(int col, int w) {
        return "<col min=\"" + col + "\" max=\"" + col + "\" width=\"" + w + "\" customWidth=\"1\"/>";
    }

    // ------------------------------------------------------------------
    // Montagem do XML da planilha
    // ------------------------------------------------------------------

    private static final class Aba {
        private final java.util.TreeMap<Integer, Linha> linhas = new java.util.TreeMap<>();

        Linha linha(int numero) {
            Linha l = linhas.get(numero);
            if (l == null) {
                l = new Linha(numero);
                linhas.put(numero, l);
            }
            return l;
        }

        String xml(String vistas, String colunas, String depoisDosDados) {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
              .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
              .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
            if (vistas != null) sb.append(vistas);
            sb.append("<sheetFormatPr defaultRowHeight=\"15\"/>");
            if (colunas != null) sb.append("<cols>").append(colunas).append("</cols>");
            sb.append("<sheetData>");
            for (Linha l : linhas.values()) sb.append(l.xml());
            sb.append("</sheetData>");
            if (depoisDosDados != null) sb.append(depoisDosDados);
            sb.append("</worksheet>");
            return sb.toString();
        }
    }

    private static final class Linha {
        private final int numero;
        private final StringBuilder celulas = new StringBuilder();

        Linha(int numero) { this.numero = numero; }

        Linha texto(int col, String s, int estilo) {
            celulas.append("<c r=\"").append(ref(col, numero)).append("\" t=\"inlineStr\" s=\"").append(estilo)
                   .append("\"><is><t xml:space=\"preserve\">").append(esc(s)).append("</t></is></c>");
            return this;
        }

        Linha numero(int col, double v, int estilo) {
            celulas.append("<c r=\"").append(ref(col, numero)).append("\" s=\"").append(estilo)
                   .append("\"><v>").append(num(v)).append("</v></c>");
            return this;
        }

        /** Fórmula + valor já calculado (quem não recalcula ao abrir mostra o valor certo). */
        Linha formula(int col, String f, double cache, int estilo) {
            celulas.append("<c r=\"").append(ref(col, numero)).append("\" s=\"").append(estilo)
                   .append("\"><f>").append(esc(f)).append("</f><v>").append(num(cache)).append("</v></c>");
            return this;
        }

        String xml() {
            return "<row r=\"" + numero + "\">" + celulas + "</row>";
        }
    }

    private static String ref(int col, int linha) {
        StringBuilder sb = new StringBuilder();
        int c = col + 1;
        while (c > 0) {
            int resto = (c - 1) % 26;
            sb.insert(0, (char) ('A' + resto));
            c = (c - 1) / 26;
        }
        return sb.append(linha).toString();
    }

    private static String num(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return String.format(Locale.US, "%.10f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                default:
                    // caracteres de controle são inválidos em XML (exceto tab/quebra de linha)
                    if (ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r') continue;
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String vazioSeNulo(String s) { return s == null ? "" : s; }

    private static void adicionar(ZipOutputStream zip, String nome, String conteudo) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        zip.write(conteudo.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    // ------------------------------------------------------------------
    // Partes fixas do pacote .xlsx
    // ------------------------------------------------------------------

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    private static final String CONTENT_TYPES = XML
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String RELS = XML
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static final String WORKBOOK_RELS = XML
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
            + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    private static String workbook(int ultimaLinhaLanc) {
        return XML
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets>"
            + "<sheet name=\"Resumo\" sheetId=\"1\" r:id=\"rId1\"/>"
            + "<sheet name=\"" + ABA_LANC + "\" sheetId=\"2\" r:id=\"rId2\"/>"
            + "</sheets>"
            + "<definedNames><definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"1\" hidden=\"1\">"
            + esc(REF_LANC) + "!$A$1:$J$" + ultimaLinhaLanc + "</definedName></definedNames>"
            + "<calcPr calcId=\"0\" fullCalcOnLoad=\"1\"/>"
            + "</workbook>";
    }

    private static final String FONTE = "<sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/>";

    private static final String STYLES = XML
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"2\">"
            + "<numFmt numFmtId=\"164\" formatCode=\"&quot;R$&quot; #,##0.00;[Red]\\-&quot;R$&quot; #,##0.00\"/>"
            + "<numFmt numFmtId=\"165\" formatCode=\"dd/mm/yyyy\"/>"
            + "</numFmts>"
            + "<fonts count=\"7\">"
            + "<font>" + FONTE + "</font>"                                              // 0 normal
            + "<font><b/>" + FONTE + "</font>"                                          // 1 negrito
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" // 2 cabeçalho
            + "<font><b/><sz val=\"14\"/><color rgb=\"FF1B5E20\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" // 3 título
            + "<font><i/><sz val=\"11\"/><color rgb=\"FF607063\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" // 4 cinza
            + "<font><sz val=\"11\"/><color rgb=\"FF2E7D32\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" // 5 receita
            + "<font><sz val=\"11\"/><color rgb=\"FFC62828\"/><name val=\"Calibri\"/><family val=\"2\"/></font>" // 6 despesa
            + "</fonts>"
            + "<fills count=\"4\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF2E7D32\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8F5E9\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"12\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"                                                   // 0 padrão
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                                    // 1 negrito
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>"                    // 2 cabeçalho
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"                          // 3 moeda
            + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"                          // 4 data
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                                    // 5 título
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>"                    // 6 total
            + "<xf numFmtId=\"164\" fontId=\"1\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\"/>" // 7 moeda total
            + "<xf numFmtId=\"10\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"                           // 8 percentual
            + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                                    // 9 cinza
            + "<xf numFmtId=\"0\" fontId=\"5\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                                    // 10 receita
            + "<xf numFmtId=\"0\" fontId=\"6\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                                    // 11 despesa
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
