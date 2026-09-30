package com.tcc.gestaorural.export;

/**
 * Superfície de desenho usada pelo layout do PDF. No app a implementação é a
 * AndroidPdfTela (PdfDocument + Canvas); manter o layout atrás desta interface
 * deixa ele em Java puro, testável e com prévia gerada fora do Android.
 *
 * Unidades em pontos (1/72 pol.); página A4 = 595 x 842. O eixo y cresce para
 * baixo e o y de texto é a linha de base.
 */
public interface Tela {
    int ESQUERDA = 0;
    int CENTRO = 1;
    int DIREITA = 2;

    void iniciarPagina(int numero);

    void finalizarPagina();

    void retangulo(float x, float y, float largura, float altura, int corArgb);

    void linha(float x1, float y1, float x2, float y2, float espessura, int corArgb);

    void texto(String s, float x, float yBase, float tamanho, boolean negrito, int corArgb, int alinhamento);

    float larguraTexto(String s, float tamanho, boolean negrito);

    /**
     * Tela que só mede texto e não desenha nada: usada numa primeira passada do
     * layout pra descobrir o total de páginas ("Página X de Y").
     */
    final class SoMedida implements Tela {
        private final Tela medidora;

        public SoMedida(Tela medidora) { this.medidora = medidora; }

        @Override public void iniciarPagina(int numero) {}
        @Override public void finalizarPagina() {}
        @Override public void retangulo(float x, float y, float l, float a, int cor) {}
        @Override public void linha(float x1, float y1, float x2, float y2, float e, int cor) {}
        @Override public void texto(String s, float x, float y, float t, boolean n, int cor, int al) {}

        @Override
        public float larguraTexto(String s, float tamanho, boolean negrito) {
            return medidora.larguraTexto(s, tamanho, negrito);
        }
    }
}
