package com.tcc.gestaorural.util;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.SystemClock;

import java.io.File;
import java.io.IOException;

/**
 * Grava a mensagem de voz do assistente.
 * - Android 10+ : Ogg/Opus (mesmo formato das mensagens de voz do WhatsApp), ~24 kbps.
 * - Android 7–9 : M4A/AAC (o Opus só existe no MediaRecorder a partir do Android 10), ~32 kbps.
 * Mono, 16 kHz (suficiente pra fala) — 1 minuto fica em ~180–240 KB.
 */
public class GravadorAudio {

    public static final int DURACAO_MAXIMA_MS = 120_000;
    public static final int DURACAO_MINIMA_MS = 1_000;

    /** Gravação pronta pra enviar. */
    public static class Gravacao {
        private final File arquivo;
        private final String mime;
        private final int duracaoSegundos;

        Gravacao(File arquivo, String mime, int duracaoSegundos) {
            this.arquivo = arquivo;
            this.mime = mime;
            this.duracaoSegundos = duracaoSegundos;
        }

        public File getArquivo() { return arquivo; }
        public String getMime() { return mime; }
        public int getDuracaoSegundos() { return duracaoSegundos; }
    }

    private MediaRecorder rec;
    private File arquivo;
    private String mime;
    private long inicio;

    public boolean gravando() { return rec != null; }

    public long msDecorridos() { return rec == null ? 0 : SystemClock.elapsedRealtime() - inicio; }

    /** Começa a gravar em cacheDir/chat. O listener avisa quando bate o limite de 2 minutos. */
    public void iniciar(Context ctx, MediaRecorder.OnInfoListener aoAtingirLimite) throws IOException {
        if (rec != null) return;
        File pasta = new File(ctx.getCacheDir(), "chat");
        if (!pasta.exists()) pasta.mkdirs();

        boolean opus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        mime = opus ? "audio/ogg" : "audio/mp4";
        arquivo = new File(pasta, "audio_" + System.currentTimeMillis() + (opus ? ".ogg" : ".m4a"));

        MediaRecorder r = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? new MediaRecorder(ctx) : new MediaRecorder();
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // checagem direta: o lint reconhece
                r.setOutputFormat(MediaRecorder.OutputFormat.OGG);
                r.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS);
                r.setAudioEncodingBitRate(24_000);
            } else {
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                r.setAudioEncodingBitRate(32_000);
            }
            r.setAudioChannels(1);
            r.setAudioSamplingRate(16_000);
            r.setMaxDuration(DURACAO_MAXIMA_MS);
            r.setOnInfoListener(aoAtingirLimite);
            r.setOutputFile(arquivo.getAbsolutePath());
            r.prepare();
            r.start();
        } catch (IOException | RuntimeException e) {
            r.release();
            arquivo.delete();
            throw new IOException("Não consegui usar o microfone.", e);
        }
        rec = r;
        inicio = SystemClock.elapsedRealtime();
    }

    /** Para e devolve a gravação; null se ficou curta demais ou falhou (o arquivo é apagado). */
    public Gravacao parar() {
        if (rec == null) return null;
        long ms = msDecorridos();
        boolean ok = true;
        try { rec.stop(); } catch (RuntimeException e) { ok = false; } // stop() falha se quase nada foi gravado
        rec.release();
        rec = null;
        if (!ok || ms < DURACAO_MINIMA_MS || !arquivo.isFile() || arquivo.length() == 0) {
            arquivo.delete();
            return null;
        }
        int seg = (int) Math.max(1, Math.round(Math.min(ms, DURACAO_MAXIMA_MS) / 1000.0));
        return new Gravacao(arquivo, mime, seg);
    }

    /** Descarta a gravação em andamento. */
    public void cancelar() {
        if (rec == null) return;
        try { rec.stop(); } catch (RuntimeException ignored) { }
        rec.release();
        rec = null;
        if (arquivo != null) arquivo.delete();
    }

    /** "0:07", "1:30". */
    public static String formatar(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + String.format(java.util.Locale.ROOT, "%02d", s % 60);
    }
}
