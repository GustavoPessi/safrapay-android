package com.tcc.gestaorural.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Arquivos dos comprovantes (fotos de nota/recibo) no armazenamento interno do app:
 * files/comprovantes/&lt;uuid&gt;.jpg — não fica em cache/, que o Android pode apagar.
 */
public final class ComprovanteUtils {

    public static final String PASTA = "comprovantes";

    private ComprovanteUtils() {}

    public static File pasta(Context ctx) {
        File p = new File(ctx.getFilesDir(), PASTA);
        if (!p.exists()) p.mkdirs();
        return p;
    }

    /** Grava os bytes JPEG num arquivo novo da pasta de comprovantes. */
    public static File salvarJpeg(Context ctx, byte[] jpeg) throws IOException {
        File f = new File(pasta(ctx), UUID.randomUUID().toString() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(f)) { out.write(jpeg); }
        return f;
    }

    /** Lê e reduz a foto pra exibir (lado maior ≈ ladoAlvo px). Rodar fora da main thread. */
    public static Bitmap miniatura(String caminho, int ladoAlvo) {
        if (caminho == null) return null;
        try {
            BitmapFactory.Options lim = new BitmapFactory.Options();
            lim.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(caminho, lim);
            if (lim.outWidth <= 0 || lim.outHeight <= 0) return null;
            int amostra = 1;
            while (Math.max(lim.outWidth, lim.outHeight) / (amostra * 2) >= ladoAlvo) amostra *= 2;
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inSampleSize = amostra;
            return BitmapFactory.decodeFile(caminho, op);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    public static boolean existe(String caminho) {
        return caminho != null && new File(caminho).isFile();
    }

    /** Abre o menu de compartilhar do Android (WhatsApp, e-mail, Drive...). */
    public static void compartilhar(Activity act, String caminho, String legenda) {
        if (!existe(caminho)) {
            Toast.makeText(act, "O arquivo do comprovante não está mais no celular.", Toast.LENGTH_LONG).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(act, act.getPackageName() + ".fileprovider", new File(caminho));
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("image/jpeg");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        if (legenda != null && !legenda.isEmpty()) i.putExtra(Intent.EXTRA_TEXT, legenda);
        i.setClipData(ClipData.newRawUri("", uri));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            act.startActivity(Intent.createChooser(i, "Compartilhar comprovante"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(act, "Nenhum app disponível para compartilhar.", Toast.LENGTH_LONG).show();
        }
    }
}
