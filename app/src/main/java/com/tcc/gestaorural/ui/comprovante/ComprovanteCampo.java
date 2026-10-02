package com.tcc.gestaorural.ui.comprovante;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.entity.Anexo;
import com.tcc.gestaorural.data.repository.AnexoRepository;
import com.tcc.gestaorural.util.AnexoUtils;
import com.tcc.gestaorural.util.ComprovanteUtils;

import java.io.File;

/**
 * Controla a seção "Comprovante" (layout view_comprovante) de uma tela.
 *
 * - Modo formulário (Nova/Editar despesa e receita): anexar/trocar/remover ficam pendentes
 *   e só valem quando o produtor toca em Salvar ({@link #aplicar(String)}).
 * - Modo imediato (Detalhe do lançamento): cada ação já grava.
 *
 * Criar no onCreate (registra os seletores de câmera/galeria).
 */
public class ComprovanteCampo {

    private static final String FOTO_CAMERA = "comprovante_camera.jpg";

    private final AppCompatActivity act;
    private final String tipo;
    private final boolean imediato;
    private final AnexoRepository repo;

    private final ImageView img;
    private final TextView txtAviso;
    private final Button btnAnexar, btnCompartilhar, btnRemover;

    private String lancamentoUuid;
    private LiveData<Anexo> observado;
    private Anexo atual;            // o que está gravado
    private File arquivoNovo;       // modo formulário: foto escolhida, ainda não salva
    private boolean removerAtual;   // modo formulário: remover o gravado ao salvar
    private String legenda;
    private String exibido;         // caminho na tela agora

    private final ActivityResultLauncher<Uri> tirarFoto;
    private final ActivityResultLauncher<String> escolherFoto;

    public ComprovanteCampo(AppCompatActivity act, String tipo, boolean imediato) {
        this.act = act;
        this.tipo = tipo;
        this.imediato = imediato;
        this.repo = new AnexoRepository(act);

        img = act.findViewById(R.id.imgComprovante);
        txtAviso = act.findViewById(R.id.txtComprovanteAviso);
        btnAnexar = act.findViewById(R.id.btnAnexarComprovante);
        btnCompartilhar = act.findViewById(R.id.btnCompartilharComprovante);
        btnRemover = act.findViewById(R.id.btnRemoverComprovante);
        if (imediato) ((TextView) act.findViewById(R.id.txtComprovanteTitulo)).setText(R.string.comprovante_tela_titulo);

        tirarFoto = act.registerForActivityResult(new ActivityResultContracts.TakePicture(), ok -> {
            if (Boolean.TRUE.equals(ok)) receberFoto(uriCamera());
        });
        escolherFoto = act.registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) receberFoto(uri);
        });

        btnAnexar.setOnClickListener(v -> escolherOrigem());
        btnCompartilhar.setOnClickListener(v -> ComprovanteUtils.compartilhar(act, exibido, legenda));
        btnRemover.setOnClickListener(v -> remover());
        img.setOnClickListener(v -> { if (exibido != null) ComprovanteActivity.abrir(act, exibido, legenda); });
        atualizar();
    }

    /** Lançamento já existente: passa a mostrar o comprovante gravado dele. */
    public void setLancamentoUuid(String uuid) {
        if (uuid == null || uuid.equals(lancamentoUuid)) return;
        lancamentoUuid = uuid;
        if (observado != null) observado.removeObservers(act);
        observado = repo.observar(tipo, uuid);
        observado.observe(act, a -> { atual = a; atualizar(); });
    }

    /** Texto que acompanha a foto ao compartilhar (ex.: "Diesel • R$ 300,00 • 30/09/2026"). */
    public void setLegenda(String legenda) { this.legenda = legenda; }

    /**
     * Modo formulário: grava as mudanças pendentes no lançamento salvo. Roda em segundo
     * plano (pode chamar logo antes do finish()).
     */
    public void aplicar(String uuidDoLancamento) {
        if (imediato || uuidDoLancamento == null) return;
        if (arquivoNovo != null) {
            repo.registrarArquivo(tipo, uuidDoLancamento, arquivoNovo, Anexo.ORIGEM_MANUAL, null);
            arquivoNovo = null;
        } else if (removerAtual) {
            repo.removerDoLancamento(tipo, uuidDoLancamento, null);
        }
        removerAtual = false;
    }

    // ------------------------------------------------------------------

    private void escolherOrigem() {
        String[] opcoes = { act.getString(R.string.chat_foto_camera), act.getString(R.string.chat_foto_galeria) };
        new AlertDialog.Builder(act)
                .setTitle(R.string.comprovante_tela_titulo)
                .setItems(opcoes, (d, qual) -> { if (qual == 0) abrirCamera(); else escolherFoto.launch("image/*"); })
                .show();
    }

    private Uri uriCamera() {
        File pasta = new File(act.getCacheDir(), "chat");
        if (!pasta.exists()) pasta.mkdirs();
        return FileProvider.getUriForFile(act, act.getPackageName() + ".fileprovider", new File(pasta, FOTO_CAMERA));
    }

    private void abrirCamera() {
        try {
            tirarFoto.launch(uriCamera());
        } catch (ActivityNotFoundException e) {
            toast("Nenhum app de câmera disponível. Escolha uma foto da galeria.");
        } catch (Exception e) {
            toast("Não consegui abrir a câmera.");
        }
    }

    /** Comprime (mesmo padrão do chat: até 1600 px, JPEG) e grava na pasta de comprovantes. */
    private void receberFoto(Uri uri) {
        toast(act.getString(R.string.comprovante_preparando));
        AppDatabase.IO.execute(() -> {
            try {
                byte[] jpeg = AnexoUtils.fotoJpeg(act, uri);
                File f = ComprovanteUtils.salvarJpeg(act, jpeg);
                act.runOnUiThread(() -> {
                    if (imediato) {
                        if (lancamentoUuid == null) { f.delete(); return; }
                        repo.registrarArquivo(tipo, lancamentoUuid, f, Anexo.ORIGEM_MANUAL,
                                ok -> { if (!ok) toast("Não consegui guardar o comprovante."); });
                    } else {
                        if (arquivoNovo != null) arquivoNovo.delete(); // trocou de novo antes de salvar
                        arquivoNovo = f;
                        removerAtual = false;
                        atualizar();
                    }
                });
            } catch (AnexoUtils.AnexoInvalido e) {
                act.runOnUiThread(() -> toast(e.getMessage()));
            } catch (Exception e) {
                act.runOnUiThread(() -> toast("Não consegui guardar o comprovante."));
            }
        });
    }

    private void remover() {
        if (!imediato && arquivoNovo != null) {
            // Foto escolhida agora e ainda não salva: só descarta.
            arquivoNovo.delete();
            arquivoNovo = null;
            atualizar();
            return;
        }
        new AlertDialog.Builder(act)
                .setMessage(R.string.comprovante_remover_pergunta)
                .setPositiveButton(R.string.comprovante_remover, (d, w) -> {
                    if (imediato) repo.removerDoLancamento(tipo, lancamentoUuid, null);
                    else { removerAtual = true; atualizar(); }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void atualizar() {
        String caminho;
        if (arquivoNovo != null) caminho = arquivoNovo.getAbsolutePath();
        else if (atual != null && !removerAtual) caminho = atual.getCaminho();
        else caminho = null;

        boolean tem = caminho != null;
        btnAnexar.setText(tem ? R.string.comprovante_trocar : R.string.comprovante_anexar);
        btnCompartilhar.setVisibility(tem ? View.VISIBLE : View.GONE);
        btnRemover.setVisibility(tem ? View.VISIBLE : View.GONE);
        txtAviso.setText(tem ? R.string.comprovante_aviso_com_foto : R.string.comprovante_aviso_local);

        if (caminho != null && caminho.equals(exibido)) return;
        exibido = caminho;
        if (!tem) { img.setImageDrawable(null); img.setVisibility(View.GONE); return; }
        img.setVisibility(View.VISIBLE);
        final String alvo = caminho;
        AppDatabase.IO.execute(() -> {
            Bitmap b = ComprovanteUtils.miniatura(alvo, 800);
            act.runOnUiThread(() -> { if (alvo.equals(exibido) && b != null) img.setImageBitmap(b); });
        });
    }

    private void toast(String msg) { Toast.makeText(act, msg, Toast.LENGTH_LONG).show(); }
}
