package com.tcc.gestaorural.ui.chat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.BaseActivity;

/** Assistente de lançamentos com IA (chat dentro do app) — fase de texto. */
public class ChatIaActivity extends BaseActivity {

    private ChatIaViewModel vm;
    private ChatAdapter adapter;
    private RecyclerView recycler;
    private EditText edtMensagem;
    private TextView txtPensando;
    private Button btnEnviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_ia);
        setTitle(R.string.chat_title);

        vm = new ViewModelProvider(this).get(ChatIaViewModel.class);

        recycler = findViewById(R.id.recycler);
        edtMensagem = findViewById(R.id.edtMensagem);
        txtPensando = findViewById(R.id.txtPensando);
        btnEnviar = findViewById(R.id.btnEnviar);

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
            txtPensando.setVisibility(e ? android.view.View.VISIBLE : android.view.View.GONE);
            btnEnviar.setEnabled(!e);
        });

        btnEnviar.setOnClickListener(v -> {
            String texto = edtMensagem.getText() == null ? "" : edtMensagem.getText().toString();
            if (texto.trim().isEmpty()) return;
            vm.enviar(texto);
            edtMensagem.setText("");
        });

        // Saudação inicial + atalhos (só na primeira criação, antes de qualquer mensagem).
        if (vm.temToken()) {
            vm.saudacaoInicial(getString(R.string.chat_bem_vindo), java.util.Arrays.asList(
                    getString(R.string.chat_sug_gastos_mes),
                    getString(R.string.chat_sug_safras_ativas),
                    getString(R.string.chat_sug_lancar_gasto),
                    getString(R.string.chat_sug_metas)));
        } else {
            vm.saudacaoInicial(getString(R.string.chat_sem_login), null);
        }
    }
}
