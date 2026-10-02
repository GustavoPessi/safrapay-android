package com.tcc.gestaorural.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.AnexoDao;
import com.tcc.gestaorural.data.local.entity.Anexo;
import com.tcc.gestaorural.util.ComprovanteUtils;
import com.tcc.gestaorural.util.DateUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Comprovantes (fotos) das despesas e receitas — Fase 1, só no celular.
 * Regra da Fase 1: um comprovante por lançamento (anexar de novo substitui o anterior).
 */
public class AnexoRepository {

    public interface Callback { void onConcluido(boolean ok); }

    /** Arquivos sem registro só são apagados depois disso (protege foto escolhida num formulário ainda aberto). */
    private static final long IDADE_MINIMA_ORFAO_MS = 6L * 60 * 60 * 1000;

    private final Context ctx;
    private final AnexoDao dao;
    private final Handler main = new Handler(Looper.getMainLooper());

    public AnexoRepository(Context context) {
        this.ctx = context.getApplicationContext();
        this.dao = AppDatabase.getInstance(ctx).anexoDao();
    }

    public LiveData<Anexo> observar(String tipo, String lancamentoUuid) {
        return dao.observarDoLancamento(tipo, lancamentoUuid);
    }

    // --- Síncronos (chamar fora da main thread) ------------------------

    /** Grava bytes JPEG como comprovante do lançamento, substituindo o que houver. */
    public Anexo gravarJpegSync(String tipo, String lancamentoUuid, byte[] jpeg, String origem) throws IOException {
        File f = ComprovanteUtils.salvarJpeg(ctx, jpeg);
        return registrarArquivoSync(tipo, lancamentoUuid, f, origem);
    }

    /** Liga um arquivo já gravado na pasta de comprovantes ao lançamento, substituindo o que houver. */
    public Anexo registrarArquivoSync(String tipo, String lancamentoUuid, File arquivo, String origem) {
        removerDoLancamentoSync(tipo, lancamentoUuid);
        String agora = DateUtils.agora();
        Anexo a = new Anexo();
        a.setUuid(UUID.randomUUID().toString());
        a.setLancamentoTipo(tipo);
        a.setLancamentoUuid(lancamentoUuid);
        a.setCaminho(arquivo.getAbsolutePath());
        a.setMime("image/jpeg");
        a.setTamanhoBytes(arquivo.length());
        a.setOrigem(origem);
        a.setCriadoEm(agora);
        a.setAtualizadoEm(agora);
        a.setSincronizado(0);
        a.setId(dao.inserir(a));
        return a;
    }

    /** Apaga os comprovantes do lançamento (linha + arquivo). */
    public void removerDoLancamentoSync(String tipo, String lancamentoUuid) {
        if (lancamentoUuid == null) return;
        for (Anexo a : dao.listarDoLancamento(tipo, lancamentoUuid)) {
            apagarArquivo(a.getCaminho());
            dao.deletar(a);
        }
    }

    /** Mais recente com arquivo existente, ou null. */
    public Anexo buscarSync(String tipo, String lancamentoUuid) {
        for (Anexo a : dao.listarDoLancamento(tipo, lancamentoUuid)) {
            if (ComprovanteUtils.existe(a.getCaminho())) return a;
        }
        return null;
    }

    /**
     * Faxina: comprovantes de lançamentos que não existem mais (ex.: safra excluída apaga
     * as despesas em cascata) e arquivos antigos da pasta que ficaram sem registro.
     */
    public void limparOrfaosSync() {
        for (Anexo a : dao.listarOrfaos()) {
            apagarArquivo(a.getCaminho());
            dao.deletar(a);
        }
        Set<String> usados = new HashSet<>(dao.listarCaminhos());
        File[] arquivos = ComprovanteUtils.pasta(ctx).listFiles();
        if (arquivos == null) return;
        long limite = System.currentTimeMillis() - IDADE_MINIMA_ORFAO_MS;
        for (File f : arquivos) {
            if (!usados.contains(f.getAbsolutePath()) && f.lastModified() < limite) f.delete();
        }
    }

    // --- Assíncronos ----------------------------------------------------

    public void removerDoLancamento(String tipo, String lancamentoUuid, Callback cb) {
        AppDatabase.IO.execute(() -> {
            removerDoLancamentoSync(tipo, lancamentoUuid);
            if (cb != null) main.post(() -> cb.onConcluido(true));
        });
    }

    public void registrarArquivo(String tipo, String lancamentoUuid, File arquivo, String origem, Callback cb) {
        AppDatabase.IO.execute(() -> {
            boolean ok;
            try { registrarArquivoSync(tipo, lancamentoUuid, arquivo, origem); ok = true; }
            catch (Exception e) { ok = false; }
            final boolean fok = ok;
            if (cb != null) main.post(() -> cb.onConcluido(fok));
        });
    }

    public void limparOrfaos() {
        AppDatabase.IO.execute(() -> { try { limparOrfaosSync(); } catch (Exception ignored) { } });
    }

    private static void apagarArquivo(String caminho) {
        if (caminho == null) return;
        File f = new File(caminho);
        if (f.exists()) f.delete();
    }
}
