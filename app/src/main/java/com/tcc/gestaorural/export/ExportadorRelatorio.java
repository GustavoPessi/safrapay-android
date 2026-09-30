package com.tcc.gestaorural.export;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.RelatorioExportDao;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Gera o arquivo do relatório por período (PDF, Excel ou CSV) a partir dos
 * dados locais (Room) e abre o menu de compartilhar do Android (WhatsApp,
 * e-mail, Drive, salvar em Arquivos...). Funciona offline.
 *
 * gerar() faz acesso ao banco e escreve arquivo: chamar fora da main thread.
 */
public final class ExportadorRelatorio {
    private ExportadorRelatorio() {}

    public enum Formato {
        PDF("pdf", "application/pdf"),
        XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        CSV("csv", "text/csv");

        public final String extensao;
        public final String mime;

        Formato(String extensao, String mime) {
            this.extensao = extensao;
            this.mime = mime;
        }
    }

    private static final String PASTA = "exports"; // precisa bater com res/xml/file_paths.xml

    public static File gerar(Context ctx, long usuarioId, String ini, String fim, Formato formato) throws IOException {
        RelatorioExportDao dao = AppDatabase.getInstance(ctx).relatorioExportDao();
        List<Lancamento> lancamentos = dao.lancamentosNoPeriodo(usuarioId, ini, fim);
        String produtor = dao.nomeUsuario(usuarioId);
        String geradoEm = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR")).format(new Date());
        RelatorioDados dados = RelatorioDados.montar(produtor, ini, fim, geradoEm, lancamentos);

        File pasta = new File(ctx.getCacheDir(), PASTA);
        if (!pasta.exists() && !pasta.mkdirs()) throw new IOException("Não foi possível criar a pasta de exportação");
        limparAntigos(pasta);

        File arquivo = new File(pasta, "safrapay-relatorio_" + ini + "_a_" + fim + "." + formato.extensao);
        FileOutputStream out = new FileOutputStream(arquivo);
        try {
            switch (formato) {
                case PDF:
                    escreverPdf(dados, out);
                    break;
                case XLSX:
                    XlsxExporter.escrever(dados, out);
                    break;
                case CSV:
                    CsvExporter.escrever(dados, out);
                    break;
            }
        } finally {
            out.close();
        }
        return arquivo;
    }

    private static void escreverPdf(RelatorioDados dados, FileOutputStream out) throws IOException {
        AndroidPdfTela tela = new AndroidPdfTela();
        try {
            // 1ª passada só mede, pra saber o total de páginas ("Página X de Y")
            int paginas = RelatorioPdfLayout.desenhar(dados, new Tela.SoMedida(tela), 0);
            RelatorioPdfLayout.desenhar(dados, tela, paginas);
            tela.salvar(out);
        } finally {
            tela.fechar();
        }
    }

    /** Apaga exportações com mais de 1 hora (já foram compartilhadas). */
    private static void limparAntigos(File pasta) {
        File[] arquivos = pasta.listFiles();
        if (arquivos == null) return;
        long limite = System.currentTimeMillis() - 60L * 60L * 1000L;
        for (File f : arquivos) {
            if (f.lastModified() < limite) //noinspection ResultOfMethodCallIgnored
                f.delete();
        }
    }

    public static void compartilhar(Activity activity, File arquivo, Formato formato) {
        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", arquivo);
        Intent envio = new Intent(Intent.ACTION_SEND);
        envio.setType(formato.mime);
        envio.putExtra(Intent.EXTRA_STREAM, uri);
        envio.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.exp_assunto));
        envio.setClipData(ClipData.newRawUri(arquivo.getName(), uri));
        envio.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(envio, activity.getString(R.string.exp_compartilhar)));
    }
}
