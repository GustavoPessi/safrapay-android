package com.tcc.gestaorural.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Prepara os anexos do assistente de IA (chat do app):
 * - foto: reduz pra no máximo 1600 px, corrige a rotação da câmera (EXIF) e comprime
 *   em JPEG — uma nota fiscal fica legível com ~300–800 KB (o servidor aceita até 5 MB);
 * - arquivo: só .xlsx, .csv e .txt, até 2 MB (limites do servidor).
 */
public final class AnexoUtils {

    public static final long MAX_BYTES_ARQUIVO = 2L * 1024 * 1024;
    private static final int LADO_MAX = 1600;
    private static final int MAX_BYTES_FOTO = 4 * 1024 * 1024;

    /** Erro com mensagem pronta pra mostrar ao produtor. */
    public static class AnexoInvalido extends Exception {
        public AnexoInvalido(String msg) { super(msg); }
    }

    private AnexoUtils() {}

    /** Foto (câmera ou galeria) → JPEG em base64, pronta pro campo "imagem". */
    public static String fotoJpegBase64(Context ctx, Uri uri) throws AnexoInvalido {
        return Base64.encodeToString(fotoJpeg(ctx, uri), Base64.NO_WRAP);
    }

    /**
     * Foto (câmera ou galeria) → bytes JPEG já reduzidos (lado maior até 1600 px) e com a
     * rotação corrigida. Usado no chat e nos comprovantes (mesma compressão nos dois).
     */
    public static byte[] fotoJpeg(Context ctx, Uri uri) throws AnexoInvalido {
        ContentResolver cr = ctx.getContentResolver();
        try {
            BitmapFactory.Options limites = new BitmapFactory.Options();
            limites.inJustDecodeBounds = true;
            try (InputStream in = cr.openInputStream(uri)) { BitmapFactory.decodeStream(in, null, limites); }
            if (limites.outWidth <= 0 || limites.outHeight <= 0) throw new AnexoInvalido("Não consegui abrir essa imagem.");

            int amostra = 1;
            while (Math.max(limites.outWidth, limites.outHeight) / (amostra * 2) >= LADO_MAX) amostra *= 2;
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inSampleSize = amostra;
            Bitmap bmp;
            try (InputStream in = cr.openInputStream(uri)) { bmp = BitmapFactory.decodeStream(in, null, op); }
            if (bmp == null) throw new AnexoInvalido("Não consegui abrir essa imagem.");

            Matrix m = new Matrix();
            float escala = Math.min(1f, (float) LADO_MAX / Math.max(bmp.getWidth(), bmp.getHeight()));
            if (escala < 1f) m.postScale(escala, escala);
            int rotacao = rotacaoExif(cr, uri);
            if (rotacao != 0) m.postRotate(rotacao);
            if (!m.isIdentity()) {
                Bitmap ajustado = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                if (ajustado != bmp) bmp.recycle();
                bmp = ajustado;
            }

            byte[] jpeg;
            int qualidade = 85;
            do {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, qualidade, out);
                jpeg = out.toByteArray();
                qualidade -= 15;
            } while (jpeg.length > MAX_BYTES_FOTO && qualidade >= 40);
            bmp.recycle();
            if (jpeg.length > MAX_BYTES_FOTO) throw new AnexoInvalido("A foto ficou grande demais. Tente outra.");
            return jpeg;
        } catch (IOException | OutOfMemoryError e) {
            throw new AnexoInvalido("Não consegui ler essa imagem.");
        }
    }

    /** Arquivo escolhido → nome + base64, validando extensão e tamanho. */
    public static String[] arquivoBase64(Context ctx, Uri uri) throws AnexoInvalido {
        String nome = nomeDoArquivo(ctx, uri);
        String n = nome.toLowerCase(Locale.ROOT);
        if (!(n.endsWith(".xlsx") || n.endsWith(".csv") || n.endsWith(".txt"))) {
            throw new AnexoInvalido("Formato não aceito. Use planilha Excel (.xlsx), .csv ou bloco de notas (.txt).");
        }
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new AnexoInvalido("Não consegui abrir esse arquivo.");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16 * 1024];
            int lidos;
            while ((lidos = in.read(buf)) != -1) {
                out.write(buf, 0, lidos);
                if (out.size() > MAX_BYTES_ARQUIVO) throw new AnexoInvalido("O arquivo passa de 2 MB. Divida em partes menores.");
            }
            if (out.size() == 0) throw new AnexoInvalido("O arquivo está vazio.");
            return new String[]{ nome, Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP) };
        } catch (IOException e) {
            throw new AnexoInvalido("Não consegui ler esse arquivo.");
        }
    }

    public static String nomeDoArquivo(Context ctx, Uri uri) {
        String nome = null;
        try (Cursor c = ctx.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) nome = c.getString(0);
        } catch (Exception ignored) { }
        if (nome == null || nome.trim().isEmpty()) {
            String seg = uri.getLastPathSegment();
            nome = seg != null ? seg : "arquivo";
        }
        return nome.trim();
    }

    private static int rotacaoExif(ContentResolver cr, Uri uri) {
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) return 0;
            android.media.ExifInterface exif = new android.media.ExifInterface(in);
            int o = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL);
            if (o == android.media.ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (o == android.media.ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (o == android.media.ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception ignored) { }
        return 0;
    }
}
