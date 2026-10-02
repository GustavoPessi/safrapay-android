package com.tcc.gestaorural.ui.chat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import com.tcc.gestaorural.data.local.entity.Anexo;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemRequestDto;
import com.tcc.gestaorural.data.repository.ChatConsultaExecutor;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.comprovante.ComprovanteActivity;
import com.tcc.gestaorural.ui.financeiro.NovaDespesaActivity;
import com.tcc.gestaorural.ui.financeiro.NovaReceitaActivity;
import com.tcc.gestaorural.util.ComprovanteUtils;
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
    private ImageButton btnAnexo, btnAcao;
    /** true = botão redondo está como "enviar" (há texto); false = microfone. */
    private boolean modoEnviar;
    private boolean enviando;
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
        btnAnexo = findViewById(R.id.btnAnexo);
        btnAcao = findViewById(R.id.btnAcao);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        adapter = new ChatAdapter(new ChatAdapter.Acao() {
            @Override public void confirmar(ChatItem item) { vm.confirmar(item); }
            @Override public void descartar(ChatItem item) { vm.descartar(item); }
            @Override public void sugestao(String texto) { vm.sugestao(texto); }
            @Override public void abrirComprovante(ChatConsultaExecutor.Comprovante c) {
                ComprovanteActivity.abrir(ChatIaActivity.this, c.getCaminho(), c.getLegenda());
            }
            @Override public void compartilharComprovante(ChatConsultaExecutor.Comprovante c) {
                ComprovanteUtils.compartilhar(ChatIaActivity.this, c.getCaminho(), c.getLegenda());
            }
            @Override public void anexarComprovante(ChatConsultaExecutor.Comprovante c) {
                // Abre a edição do lançamento (lá tem a seção Comprovante).
                Class<?> alvo = Anexo.TIPO_RECEITA.equals(c.getTipo()) ? NovaReceitaActivity.class : NovaDespesaActivity.class;
                Intent i = new Intent(ChatIaActivity.this, alvo);
                i.putExtra(NavKeys.LANCAMENTO_ID, c.getLancamentoId());
                startActivity(i);
            }
        });
        recycler.setAdapter(adapter);

        vm.getLista().observe(this, itens -> {
            adapter.submeter(itens);
            if (itens != null && !itens.isEmpty()) recycler.scrollToPosition(itens.size() - 1);
        });

        vm.getEnviando().observe(this, enviando -> {
            this.enviando = Boolean.TRUE.equals(enviando);
            txtPensando.setVisibility(this.enviando ? View.VISIBLE : View.GONE);
            btnAnexo.setEnabled(!this.enviando);
            btnAnexo.setAlpha(this.enviando ? 0.4f : 1f);
            atualizarBotaoAcao();
        });

        edtMensagem.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { atualizarBotaoAcao(); }
        });
        atualizarBotaoAcao();

        btnAcao.setOnClickListener(v -> {
            if (!modoEnviar) { ouvir(); return; }
            String texto = textoDigitado();
            if (texto.isEmpty()) return;
            vm.enviar(texto);
            edtMensagem.setText("");
        });
        btnAnexo.setOnClickListener(v -> escolherAnexo());

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

    /** Microfone com o campo vazio; seta de enviar quando há texto (igual ao WhatsApp). */
    private void atualizarBotaoAcao() {
        modoEnviar = !textoDigitado().isEmpty();
        btnAcao.setImageResource(modoEnviar ? R.drawable.ic_chat_enviar : R.drawable.ic_chat_mic_branco);
        btnAcao.setContentDescription(getString(modoEnviar ? R.string.chat_enviar : R.string.chat_audio));
        // Falar continua liberado enquanto a IA responde (só preenche o campo); enviar, não.
        btnAcao.setEnabled(!(modoEnviar && enviando));
    }

    // --- Anexos (clipe) ------------------------------------------------

    private void escolherAnexo() {
        if (vm.estaEnviando()) { toast(getString(R.string.chat_aguarde)); return; }
        String[] opcoes = {
                getString(R.string.chat_foto_camera),
                getString(R.string.chat_foto_galeria),
                getString(R.string.chat_anexo_arquivo) };
        new AlertDialog.Builder(this)
                .setTitle(R.string.chat_anexar_titulo)
                .setItems(opcoes, (d, qual) -> {
                    if (qual == 0) abrirCamera();
                    else if (qual == 1) escolherFoto.launch("image/*");
                    else abrirSeletorArquivo();
                })
                .show();
    }

    private void abrirSeletorArquivo() {
        try { escolherArquivo.launch(TIPOS_ARQUIVO); }
        catch (ActivityNotFoundException e) { toast("Nenhum app de arquivos disponível."); }
    }

    // --- Foto ----------------------------------------------------------

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
                byte[] jpeg = AnexoUtils.fotoJpeg(this, uri);
                String base64 = android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP);
                runOnUiThread(() -> {
                    edtMensagem.setText("");
                    vm.enviarAnexo(legenda, getString(R.string.chat_rotulo_foto),
                            new ChatMensagemRequestDto.Imagem("image/jpeg", base64), null, jpeg);
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
                            new ChatMensagemRequestDto.Arquivo(nomeEConteudo[0], nomeEConteudo[1]), null);
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
