package com.tcc.gestaorural.ui.chat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemRequestDto;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.util.AnexoUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Assistente de IA (chat dentro do app): texto, foto (câmera/galeria), arquivo
 * (.xlsx/.csv/.txt) e áudio. O áudio usa o reconhecimento de voz do próprio
 * aparelho (fala → texto no campo, pra o produtor conferir antes de enviar) —
 * o servidor não precisa transcrever nada.
 */
public class ChatIaActivity extends BaseActivity {

    private static final String ESTADO_FOTO_URI = "foto_camera_uri";
    /** Tipos aceitos no seletor de arquivo (a extensão é conferida de novo depois de escolher). */
    private static final String[] TIPOS_ARQUIVO = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv", "text/comma-separated-values", "application/csv",
            "text/plain", "application/vnd.ms-excel", "application/octet-stream"
    };

    private ChatIaViewModel vm;
    private ChatAdapter adapter;
    private RecyclerView recycler;
    private EditText edtMensagem;
    private TextView txtPensando;
    private Button btnEnviar;
    private ImageButton btnFoto, btnArquivo, btnAudio;
    private Uri fotoCameraUri;

    private final ActivityResultLauncher<Uri> tirarFoto = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            ok -> { if (Boolean.TRUE.equals(ok) && fotoCameraUri != null) prepararFoto(fotoCameraUri); });

    private final ActivityResultLauncher<String> escolherFoto = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> { if (uri != null) prepararFoto(uri); });

    private final ActivityResultLauncher<String[]> escolherArquivo = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) prepararArquivo(uri); });

    private final ActivityResultLauncher<Intent> falar = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() != RESULT_OK || resultado.getData() == null) return;
                ArrayList<String> textos = resultado.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (textos != null && !textos.isEmpty()) inserirTexto(textos.get(0));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_ia);
        setTitle(R.string.chat_title);
        if (savedInstanceState != null) {
            String u = savedInstanceState.getString(ESTADO_FOTO_URI);
            if (u != null) fotoCameraUri = Uri.parse(u);
        }

        vm = new ViewModelProvider(this).get(ChatIaViewModel.class);

        recycler = findViewById(R.id.recycler);
        edtMensagem = findViewById(R.id.edtMensagem);
        txtPensando = findViewById(R.id.txtPensando);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnFoto = findViewById(R.id.btnFoto);
        btnArquivo = findViewById(R.id.btnArquivo);
        btnAudio = findViewById(R.id.btnAudio);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        adapter = new ChatAdapter(new ChatAdapter.Acao() {
            @Override public void confirmar(ChatItem item) { vm.confirmar(item); }
            @Override public void descartar(ChatItem item) { vm.descartar(item); }
            @Override public void sugestao(String texto) { vm.sugestao(texto); }
        });
        recycler.setAdapter(adapter);

        vm.getLista().observe(this, itens -> {
            adapter.submeter(itens);
            if (itens != null && !itens.isEmpty()) recycler.scrollToPosition(itens.size() - 1);
        });

        vm.getEnviando().observe(this, enviando -> {
            boolean e = Boolean.TRUE.equals(enviando);
            txtPensando.setVisibility(e ? View.VISIBLE : View.GONE);
            btnEnviar.setEnabled(!e);
            btnFoto.setEnabled(!e);
            btnArquivo.setEnabled(!e);
            btnFoto.setAlpha(e ? 0.4f : 1f);
            btnArquivo.setAlpha(e ? 0.4f : 1f);
        });

        btnEnviar.setOnClickListener(v -> {
            String texto = textoDigitado();
            if (texto.isEmpty()) return;
            vm.enviar(texto);
            edtMensagem.setText("");
        });
        btnFoto.setOnClickListener(v -> escolherOrigemFoto());
        btnArquivo.setOnClickListener(v -> {
            if (vm.estaEnviando()) { toast(getString(R.string.chat_aguarde)); return; }
            try { escolherArquivo.launch(TIPOS_ARQUIVO); }
            catch (ActivityNotFoundException e) { toast("Nenhum app de arquivos disponível."); }
        });
        btnAudio.setOnClickListener(v -> ouvir());

        // Saudação inicial + atalhos (só na primeira criação, antes de qualquer mensagem).
        if (vm.temToken()) {
            vm.saudacaoInicial(getString(R.string.chat_bem_vindo), Arrays.asList(
                    getString(R.string.chat_sug_gastos_mes),
                    getString(R.string.chat_sug_safras_ativas),
                    getString(R.string.chat_sug_lancar_gasto),
                    getString(R.string.chat_sug_metas)));
        } else {
            vm.saudacaoInicial(getString(R.string.chat_sem_login), null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (fotoCameraUri != null) outState.putString(ESTADO_FOTO_URI, fotoCameraUri.toString());
    }

    // --- Foto ----------------------------------------------------------

    private void escolherOrigemFoto() {
        if (vm.estaEnviando()) { toast(getString(R.string.chat_aguarde)); return; }
        String[] opcoes = { getString(R.string.chat_foto_camera), getString(R.string.chat_foto_galeria) };
        new AlertDialog.Builder(this)
                .setTitle(R.string.chat_foto_titulo)
                .setItems(opcoes, (d, qual) -> { if (qual == 0) abrirCamera(); else escolherFoto.launch("image/*"); })
                .show();
    }

    private void abrirCamera() {
        try {
            File pasta = new File(getCacheDir(), "chat");
            if (!pasta.exists()) pasta.mkdirs();
            File[] antigas = pasta.listFiles();
            if (antigas != null) for (File f : antigas) f.delete(); // não acumula fotos no cache
            File foto = new File(pasta, "foto_" + System.currentTimeMillis() + ".jpg");
            fotoCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", foto);
            tirarFoto.launch(fotoCameraUri);
        } catch (ActivityNotFoundException e) {
            toast("Nenhum app de câmera disponível. Escolha uma foto da galeria.");
        } catch (Exception e) {
            toast("Não consegui abrir a câmera.");
        }
    }

    private void prepararFoto(Uri uri) {
        final String legenda = textoDigitado();
        toast(getString(R.string.chat_preparando_anexo));
        AppDatabase.IO.execute(() -> {
            try {
                String base64 = AnexoUtils.fotoJpegBase64(this, uri);
                runOnUiThread(() -> {
                    edtMensagem.setText("");
                    vm.enviarAnexo(legenda, getString(R.string.chat_rotulo_foto),
                            new ChatMensagemRequestDto.Imagem("image/jpeg", base64), null);
                });
            } catch (AnexoUtils.AnexoInvalido e) {
                runOnUiThread(() -> toast(e.getMessage()));
            }
        });
    }

    // --- Arquivo -------------------------------------------------------

    private void prepararArquivo(Uri uri) {
        final String legenda = textoDigitado();
        toast(getString(R.string.chat_preparando_anexo));
        AppDatabase.IO.execute(() -> {
            try {
                String[] nomeEConteudo = AnexoUtils.arquivoBase64(this, uri);
                runOnUiThread(() -> {
                    edtMensagem.setText("");
                    vm.enviarAnexo(legenda, getString(R.string.chat_rotulo_arquivo, nomeEConteudo[0]), null,
                            new ChatMensagemRequestDto.Arquivo(nomeEConteudo[0], nomeEConteudo[1]));
                });
            } catch (AnexoUtils.AnexoInvalido e) {
                runOnUiThread(() -> toast(e.getMessage()));
            }
        });
    }

    // --- Áudio (voz → texto no aparelho) -------------------------------

    private void ouvir() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.chat_audio_prompt));
        try {
            falar.launch(i);
        } catch (ActivityNotFoundException e) {
            toast(getString(R.string.chat_audio_indisponivel));
        }
    }

    /** Coloca o que foi falado no campo (o produtor confere e toca em Enviar). */
    private void inserirTexto(String falado) {
        if (falado == null || falado.trim().isEmpty()) return;
        String t = falado.trim();
        t = t.substring(0, 1).toUpperCase(new Locale("pt", "BR")) + t.substring(1);
        String atual = textoDigitado();
        edtMensagem.setText(atual.isEmpty() ? t : atual + " " + t);
        edtMensagem.setSelection(edtMensagem.getText().length());
        edtMensagem.requestFocus();
    }

    // -------------------------------------------------------------------

    private String textoDigitado() {
        return edtMensagem.getText() == null ? "" : edtMensagem.getText().toString().trim();
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); }
}
