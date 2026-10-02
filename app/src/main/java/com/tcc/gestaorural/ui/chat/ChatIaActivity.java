package com.tcc.gestaorural.ui.chat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.core.content.ContextCompat;
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
import com.tcc.gestaorural.util.GravadorAudio;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.util.AnexoUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Assistente de IA (chat dentro do app): texto, foto (câmera/galeria), arquivo
 * (.xlsx/.csv/.txt) e mensagem de voz. A voz é gravada como áudio de verdade
 * (segurar o microfone, soltar pra enviar, deslizar pra esquerda pra cancelar) e
 * transcrita no servidor — o mesmo caminho que as mensagens de voz do WhatsApp vão usar.
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

    // Mensagem de voz
    private final GravadorAudio gravador = new GravadorAudio();
    private final Handler relogio = new Handler(Looper.getMainLooper());
    private TextView txtGravando;
    private float xInicioToque;
    private MediaPlayer player;
    private String audioTocando;

    private final ActivityResultLauncher<Uri> tirarFoto = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            ok -> { if (Boolean.TRUE.equals(ok) && fotoCameraUri != null) prepararFoto(fotoCameraUri); });

    private final ActivityResultLauncher<String> escolherFoto = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> { if (uri != null) prepararFoto(uri); });

    private final ActivityResultLauncher<String[]> escolherArquivo = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) prepararArquivo(uri); });

    private final ActivityResultLauncher<String> pedirMicrofone = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), ok -> {
                toast(getString(Boolean.TRUE.equals(ok) ? R.string.chat_segure_para_gravar : R.string.chat_permissao_microfone));
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
            @Override public void tocarAudio(ChatItem item) { ChatIaActivity.this.tocarAudio(item); }
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

        txtGravando = findViewById(R.id.txtGravando);
        btnAcao.setOnTouchListener(this::toqueMicrofone);
        btnAcao.setOnClickListener(v -> {
            if (!modoEnviar) return; // microfone: tratado no toque (segurar pra gravar)
            String texto = textoDigitado();
            if (texto.isEmpty()) return;
            vm.enviar(texto);
            edtMensagem.setText("");
        });
        btnAnexo.setOnClickListener(v -> escolherAnexo());

        if (savedInstanceState == null) limparAudiosAntigos();

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
        btnAcao.setContentDescription(getString(modoEnviar ? R.string.chat_enviar : R.string.chat_gravar_audio));
        // Enquanto a IA responde, dá pra digitar, mas não enviar nem gravar outra mensagem.
        btnAcao.setEnabled(!enviando);
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
            if (antigas != null) for (File f : antigas) {
                if (f.getName().startsWith("foto_")) f.delete(); // não acumula fotos no cache (áudios ficam: tocam no chat)
            }
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

    // --- Mensagem de voz (segurar pra gravar) -------------------------

    /** Segurar = grava; soltar = envia; deslizar pra esquerda = cancela. Com texto no campo, é o "enviar". */
    @SuppressLint("ClickableViewAccessibility")
    private boolean toqueMicrofone(View v, MotionEvent ev) {
        if (modoEnviar) return false; // deixa o onClick (enviar texto) funcionar
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                xInicioToque = ev.getRawX();
                iniciarGravacao();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (gravador.gravando() && xInicioToque - ev.getRawX() > dp(90)) {
                    cancelarGravacao();
                    toast(getString(R.string.chat_audio_cancelado));
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (gravador.gravando()) pararEEnviar();
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (gravador.gravando()) cancelarGravacao();
                return true;
            default:
                return true;
        }
    }

    private void iniciarGravacao() {
        if (vm.estaEnviando()) { toast(getString(R.string.chat_aguarde)); return; }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            pedirMicrofone.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        pararAudio();
        try {
            gravador.iniciar(this, (mr, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) pararEEnviar();
            });
        } catch (Exception e) {
            toast(getString(R.string.chat_audio_erro_gravar));
            return;
        }
        edtMensagem.setVisibility(View.INVISIBLE);
        txtGravando.setVisibility(View.VISIBLE);
        btnAnexo.setEnabled(false);
        btnAnexo.setAlpha(0.4f);
        btnAcao.setScaleX(1.2f);
        btnAcao.setScaleY(1.2f);
        atualizarRelogio.run();
    }

    private final Runnable atualizarRelogio = new Runnable() {
        @Override public void run() {
            if (!gravador.gravando()) return;
            txtGravando.setText(getString(R.string.chat_gravando, GravadorAudio.formatar(gravador.msDecorridos())));
            relogio.postDelayed(this, 250);
        }
    };

    private void fimDaGravacaoNaTela() {
        relogio.removeCallbacks(atualizarRelogio);
        txtGravando.setVisibility(View.GONE);
        edtMensagem.setVisibility(View.VISIBLE);
        btnAnexo.setEnabled(!enviando);
        btnAnexo.setAlpha(enviando ? 0.4f : 1f);
        btnAcao.setScaleX(1f);
        btnAcao.setScaleY(1f);
    }

    private void cancelarGravacao() {
        gravador.cancelar();
        fimDaGravacaoNaTela();
    }

    private void pararEEnviar() {
        GravadorAudio.Gravacao g = gravador.parar();
        fimDaGravacaoNaTela();
        if (g == null) { toast(getString(R.string.chat_segure_para_gravar)); return; }
        AppDatabase.IO.execute(() -> {
            try {
                byte[] bytes = new byte[(int) g.getArquivo().length()]; // máx. ~500 KB (2 min)
                try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.FileInputStream(g.getArquivo()))) {
                    in.readFully(bytes); // java.nio.file.Files só existe a partir do Android 8 (minSdk é 7)
                }
                String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                runOnUiThread(() -> vm.enviarAudio(g.getArquivo().getAbsolutePath(), g.getMime(),
                        g.getDuracaoSegundos(), base64));
            } catch (Exception e) {
                runOnUiThread(() -> toast(getString(R.string.chat_audio_erro_gravar)));
            }
        });
    }

    /** Toca/pausa a mensagem de voz da bolha (uma de cada vez). */
    private void tocarAudio(ChatItem item) {
        String caminho = item.getCaminhoAudio();
        if (caminho == null) return;
        if (caminho.equals(audioTocando)) { pararAudio(); return; }
        pararAudio();
        if (!new File(caminho).isFile()) { toast(getString(R.string.chat_audio_nao_encontrado)); return; }
        try {
            player = new MediaPlayer();
            player.setDataSource(caminho);
            player.setOnCompletionListener(mp -> pararAudio());
            player.prepare();
            player.start();
            audioTocando = caminho;
            adapter.setAudioTocando(caminho);
        } catch (Exception e) {
            pararAudio();
            toast(getString(R.string.chat_audio_nao_encontrado));
        }
    }

    private void pararAudio() {
        if (player != null) {
            try { player.stop(); } catch (IllegalStateException ignored) { }
            player.release();
            player = null;
        }
        if (audioTocando != null) {
            audioTocando = null;
            adapter.setAudioTocando(null);
        }
    }

    /** Áudios antigos do cache (de conversas passadas, que já não aparecem na tela). */
    private void limparAudiosAntigos() {
        AppDatabase.IO.execute(() -> {
            File[] arquivos = new File(getCacheDir(), "chat").listFiles();
            if (arquivos == null) return;
            long limite = System.currentTimeMillis() - 24L * 60 * 60 * 1000;
            for (File f : arquivos) if (f.getName().startsWith("audio_") && f.lastModified() < limite) f.delete();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (gravador.gravando()) cancelarGravacao(); // app foi pro fundo no meio da gravação
        pararAudio();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // -------------------------------------------------------------------

    private String textoDigitado() {
        return edtMensagem.getText() == null ? "" : edtMensagem.getText().toString().trim();
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); }
}
