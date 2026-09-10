package com.tcc.gestaorural.ui.maquina;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.util.MaquinaCatalogo;

import java.util.List;

public class NovaMaquinaActivity extends BaseActivity {

    private MaquinaViewModel vm;
    private MaquinaCatalogo catalogo;
    private TextInputEditText edtNome, edtAno, edtIdentificacao, edtValor, edtObservacoes;
    private MaterialAutoCompleteTextView dropTipo, dropMarca, dropModelo;

    private long maquinaId = 0L;
    private Maquina existente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_maquina);
        vm = new ViewModelProvider(this).get(MaquinaViewModel.class);
        catalogo = MaquinaCatalogo.get(this);

        edtNome = findViewById(R.id.edtNome);
        edtAno = findViewById(R.id.edtAno);
        edtIdentificacao = findViewById(R.id.edtIdentificacao);
        edtValor = findViewById(R.id.edtValor);
        edtObservacoes = findViewById(R.id.edtObservacoes);
        dropTipo = findViewById(R.id.dropTipo);
        dropMarca = findViewById(R.id.dropMarca);
        dropModelo = findViewById(R.id.dropModelo);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        maquinaId = getIntent().getLongExtra(NavKeys.MAQUINA_ID, 0L);
        setTitle(maquinaId == 0L ? R.string.maquina_nova_title : R.string.maquina_editar_title);

        dropTipo.setAdapter(adapter(catalogo.tipos()));
        dropTipo.setOnItemClickListener((p, v, pos, id) -> {
            atualizarMarcas();
            dropMarca.setText("", false);
            dropModelo.setText("", false);
            dropModelo.setAdapter(adapter(new java.util.ArrayList<>()));
        });
        dropMarca.setOnItemClickListener((p, v, pos, id) -> {
            atualizarModelos();
            dropModelo.setText("", false);
        });
        dropModelo.setOnItemClickListener((p, v, pos, id) -> {
            if (txt(edtNome).isEmpty()) {
                String tipo = dropTipo.getText().toString().trim();
                String modelo = dropModelo.getText().toString().trim();
                edtNome.setText((tipo.isEmpty() ? "" : tipo + " ") + modelo);
            }
        });

        if (maquinaId != 0L) {
            vm.obter(maquinaId).observe(this, m -> {
                if (m != null && existente == null) { existente = m; preencher(m); }
            });
        }

        btnSalvar.setOnClickListener(v -> salvar());
        vm.getSalvoComId().observe(this, id -> {
            if (id == null) return;
            Toast.makeText(this, R.string.msg_maquina_salva, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void atualizarMarcas() {
        dropMarca.setAdapter(adapter(catalogo.marcas(dropTipo.getText().toString().trim())));
    }
    private void atualizarModelos() {
        dropModelo.setAdapter(adapter(catalogo.modelos(
                dropTipo.getText().toString().trim(), dropMarca.getText().toString().trim())));
    }

    private ArrayAdapter<String> adapter(List<String> dados) {
        return new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dados);
    }

    private void preencher(Maquina m) {
        edtNome.setText(m.nome);
        edtAno.setText(m.ano);
        edtIdentificacao.setText(m.identificacao);
        edtValor.setText(m.valor == null ? "" : String.valueOf(m.valor));
        edtObservacoes.setText(m.observacoes);
        if (m.tipo != null) dropTipo.setText(m.tipo, false);
        atualizarMarcas();
        if (m.marca != null) dropMarca.setText(m.marca, false);
        atualizarModelos();
        if (m.modelo != null) dropModelo.setText(m.modelo, false);
    }

    private void salvar() {
        String nome = txt(edtNome);
        if (nome.isEmpty()) { Toast.makeText(this, R.string.err_maq_nome, Toast.LENGTH_LONG).show(); return; }
        Maquina m = (existente != null) ? existente : new Maquina();
        m.id = maquinaId;
        m.usuarioId = new com.tcc.gestaorural.util.SessionManager(this).getUsuarioId();
        m.nome = nome;
        m.tipo = vazioNull(dropTipo.getText().toString());
        m.marca = vazioNull(dropMarca.getText().toString());
        m.modelo = vazioNull(dropModelo.getText().toString());
        m.ano = vazioNull(txt(edtAno));
        m.identificacao = vazioNull(txt(edtIdentificacao));
        m.valor = com.tcc.gestaorural.util.MoneyUtils.parse(txt(edtValor));
        m.observacoes = vazioNull(txt(edtObservacoes));
        vm.salvar(m);
    }

    private String vazioNull(String s) { s = s == null ? "" : s.trim(); return s.isEmpty() ? null : s; }
    private String txt(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
}
