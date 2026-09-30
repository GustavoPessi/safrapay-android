package com.tcc.gestaorural.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * CSV dos lançamentos do período, no padrão que o Excel em português abre
 * direto com duplo clique: separador ";", vírgula decimal, UTF-8 com BOM
 * (senão os acentos quebram no Excel) e quebra de linha CRLF.
 */
public final class CsvExporter {
    private CsvExporter() {}

    public static final String[] CABECALHO = {
        "Data", "Tipo", "Descrição", "Valor", "Propriedade", "Talhão", "Cultura", "Safra", "Categoria", "Máquina"
    };

    public static void escrever(RelatorioDados r, OutputStream out) throws IOException {
        Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        w.write('﻿');
        linha(w, CABECALHO);
        for (Lancamento l : r.lancamentos) {
            linha(w, new String[] {
                Formatos.dataBr(l.data),
                l.tipoPorExtenso(),
                l.descricao,
                Formatos.numeroCsv(l.valor),
                l.propriedade,
                l.talhao,
                l.cultura,
                l.safra,
                l.categoria,
                l.maquina
            });
        }
        w.flush();
    }

    private static void linha(Writer w, String[] campos) throws IOException {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) w.write(';');
            w.write(campo(campos[i]));
        }
        w.write("\r\n");
    }

    private static String campo(String s) {
        if (s == null) return "";
        boolean precisaAspas = s.indexOf(';') >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!precisaAspas) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
