package com.tcc.gestaorural.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Planilha .xlsx genérica (várias abas, cabeçalho + linhas), sem biblioteca externa — mesmo
 * jeito do XlsxExporter (o .xlsx é um .zip de XMLs). Usada no "Exportar meus dados".
 * Células: String → texto; Number → número (coluna marcada como moeda sai em R$); null → vazia.
 */
public final class PlanilhaXlsx {

    private static final int S_PADRAO = 0, S_CABECALHO = 1, S_MOEDA = 2, S_TITULO = 3, S_CINZA = 4;

    private static final class Aba {
        String nome;
        String[] cabecalho;
        List<Object[]> linhas;
        boolean[] moeda;
    }

    private final List<Aba> abas = new ArrayList<>();

    /** Aba de tabela: cabeçalho fixo, filtro e colunas de moeda (true = formato R$). */
    public PlanilhaXlsx aba(String nome, String[] cabecalho, List<Object[]> linhas, boolean[] moeda) {
        Aba a = new Aba();
        a.nome = nomeValido(nome);
        a.cabecalho = cabecalho;
        a.linhas = linhas;
        a.moeda = moeda;
        abas.add(a);
        return this;
    }

    /** Aba de texto (título na 1ª linha, uma linha por item, sem cabeçalho de tabela). */
    public PlanilhaXlsx abaTexto(String nome, List<String> linhas) {
        Aba a = new Aba();
        a.nome = nomeValido(nome);
        a.linhas = new ArrayList<>();
        for (String l : linhas) a.linhas.add(new Object[]{ l });
        abas.add(a);
        return this;
    }

    public void escrever(OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        adicionar(zip, "[Content_Types].xml", contentTypes());
        adicionar(zip, "_rels/.rels", XML + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>");
        adicionar(zip, "xl/workbook.xml", workbook());
        adicionar(zip, "xl/_rels/workbook.xml.rels", workbookRels());
        adicionar(zip, "xl/styles.xml", STYLES);
        for (int i = 0; i < abas.size(); i++) adicionar(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", aba(abas.get(i)));
        zip.finish();
        zip.flush();
    }

    // ------------------------------------------------------------------

    private static String aba(Aba a) {
        StringBuilder sb = new StringBuilder(XML)
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");
        boolean tabela = a.cabecalho != null;
        if (tabela) {
            sb.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
            sb.append("<cols>");
            for (int c = 0; c < a.cabecalho.length; c++) {
                int w = Math.max(12, Math.min(45, a.cabecalho[c].length() + 4));
                for (Object[] l : a.linhas) if (c < l.length && l[c] instanceof String) w = Math.max(w, Math.min(45, ((String) l[c]).length() + 2));
                sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1).append("\" width=\"").append(w).append("\" customWidth=\"1\"/>");
            }
            sb.append("</cols>");
        } else {
            sb.append("<cols><col min=\"1\" max=\"1\" width=\"120\" customWidth=\"1\"/></cols>");
        }
        sb.append("<sheetData>");
        int linha = 1;
        if (tabela) {
            sb.append("<row r=\"1\">");
            for (int c = 0; c < a.cabecalho.length; c++) sb.append(texto(c, 1, a.cabecalho[c], S_CABECALHO));
            sb.append("</row>");
            linha = 2;
        }
        for (int i = 0; i < a.linhas.size(); i++) {
            Object[] l = a.linhas.get(i);
            sb.append("<row r=\"").append(linha).append("\">");
            for (int c = 0; c < l.length; c++) {
                Object v = l[c];
                if (v == null) continue;
                if (v instanceof Number) {
                    boolean m = a.moeda != null && c < a.moeda.length && a.moeda[c];
                    sb.append("<c r=\"").append(ref(c, linha)).append("\" s=\"").append(m ? S_MOEDA : S_PADRAO)
                      .append("\"><v>").append(num(((Number) v).doubleValue())).append("</v></c>");
                } else {
                    int estilo = tabela ? S_PADRAO : (i == 0 ? S_TITULO : (String.valueOf(v).startsWith("•") ? S_PADRAO : S_CINZA));
                    sb.append(texto(c, linha, String.valueOf(v), estilo));
                }
            }
            sb.append("</row>");
            linha++;
        }
        sb.append("</sheetData>");
        if (tabela && a.cabecalho.length > 0) {
            sb.append("<autoFilter ref=\"A1:").append(ref(a.cabecalho.length - 1, Math.max(1, a.linhas.size() + 1))).append("\"/>");
        }
        sb.append("</worksheet>");
        return sb.toString();
    }

    private static String texto(int col, int linha, String s, int estilo) {
        return "<c r=\"" + ref(col, linha) + "\" t=\"inlineStr\" s=\"" + estilo + "\"><is><t xml:space=\"preserve\">"
                + esc(s) + "</t></is></c>";
    }

    private String contentTypes() {
        StringBuilder sb = new StringBuilder(XML)
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= abas.size(); i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
              .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return sb.append("</Types>").toString();
    }

    private String workbook() {
        StringBuilder sb = new StringBuilder(XML)
                .append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < abas.size(); i++) {
            sb.append("<sheet name=\"").append(esc(abas.get(i).nome)).append("\" sheetId=\"").append(i + 1)
              .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return sb.append("</sheets></workbook>").toString();
    }

    private String workbookRels() {
        StringBuilder sb = new StringBuilder(XML)
                .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= abas.size(); i++) {
            sb.append("<Relationship Id=\"rId").append(i)
              .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
              .append(i).append(".xml\"/>");
        }
        sb.append("<Relationship Id=\"rId").append(abas.size() + 1)
          .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return sb.append("</Relationships>").toString();
    }

    /** Nome de aba: até 31 caracteres e sem : \ / ? * [ ] (regra do Excel). */
    private static String nomeValido(String n) {
        String s = n.replaceAll("[:\\\\/?*\\[\\]]", " ").trim();
        return s.length() > 31 ? s.substring(0, 31) : s;
    }

    private static String ref(int col, int linha) {
        StringBuilder sb = new StringBuilder();
        int c = col + 1;
        while (c > 0) { int r = (c - 1) % 26; sb.insert(0, (char) ('A' + r)); c = (c - 1) / 26; }
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
                    if (ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r') continue;
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void adicionar(ZipOutputStream zip, String nome, String conteudo) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        zip.write(conteudo.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    private static final String FONTE = "<sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/>";

    private static final String STYLES = XML
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"&quot;R$&quot; #,##0.00;[Red]\\-&quot;R$&quot; #,##0.00\"/></numFmts>"
            + "<fonts count=\"4\">"
            + "<font>" + FONTE + "</font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"14\"/><color rgb=\"FF1B5E20\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><i/><sz val=\"11\"/><color rgb=\"FF607063\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "</fonts>"
            + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF2E7D32\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"5\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>"
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
