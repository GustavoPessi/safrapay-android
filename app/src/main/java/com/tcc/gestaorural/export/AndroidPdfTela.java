package com.tcc.gestaorural.export;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.IOException;
import java.io.OutputStream;

/** Implementação da Tela sobre o PdfDocument nativo do Android (API 19+). */
public class AndroidPdfTela implements Tela {

    private final PdfDocument documento = new PdfDocument();
    private final Paint pincelTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelForma = new Paint(Paint.ANTI_ALIAS_FLAG);
    private PdfDocument.Page pagina;
    private Canvas canvas;

    public AndroidPdfTela() {
        pincelTexto.setSubpixelText(true);
    }

    @Override
    public void iniciarPagina(int numero) {
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                (int) RelatorioPdfLayout.LARGURA, (int) RelatorioPdfLayout.ALTURA, numero).create();
        pagina = documento.startPage(info);
        canvas = pagina.getCanvas();
    }

    @Override
    public void finalizarPagina() {
        if (pagina != null) documento.finishPage(pagina);
        pagina = null;
        canvas = null;
    }

    @Override
    public void retangulo(float x, float y, float largura, float altura, int corArgb) {
        if (largura <= 0 || altura <= 0) return;
        pincelForma.setStyle(Paint.Style.FILL);
        pincelForma.setColor(corArgb);
        canvas.drawRect(x, y, x + largura, y + altura, pincelForma);
    }

    @Override
    public void linha(float x1, float y1, float x2, float y2, float espessura, int corArgb) {
        pincelForma.setStyle(Paint.Style.STROKE);
        pincelForma.setStrokeWidth(espessura);
        pincelForma.setColor(corArgb);
        canvas.drawLine(x1, y1, x2, y2, pincelForma);
    }

    @Override
    public void texto(String s, float x, float yBase, float tamanho, boolean negrito, int corArgb, int alinhamento) {
        configurar(tamanho, negrito);
        pincelTexto.setColor(corArgb);
        pincelTexto.setTextAlign(alinhamento == DIREITA ? Paint.Align.RIGHT
                : alinhamento == CENTRO ? Paint.Align.CENTER : Paint.Align.LEFT);
        canvas.drawText(s, x, yBase, pincelTexto);
    }

    @Override
    public float larguraTexto(String s, float tamanho, boolean negrito) {
        configurar(tamanho, negrito);
        return pincelTexto.measureText(s);
    }

    private void configurar(float tamanho, boolean negrito) {
        pincelTexto.setTextSize(tamanho);
        pincelTexto.setTypeface(negrito ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

    public void salvar(OutputStream out) throws IOException {
        documento.writeTo(out);
    }

    public void fechar() {
        documento.close();
    }
}
