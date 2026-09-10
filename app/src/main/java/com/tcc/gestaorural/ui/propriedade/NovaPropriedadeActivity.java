package com.tcc.gestaorural.ui.propriedade;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Propriedade;
import com.tcc.gestaorural.util.SessionManager;

public class NovaPropriedadeActivity extends BaseActivity {

    private PropriedadeViewModel vm;
    private TextInputEditText edtNome, edtLocalidade, edtArea, edtTipoSolo, edtMatricula, edtCib, edtValor, edtObservacoes;
    private Button btnSalvar;

    private long propriedadeId = 0L;      // 0 = nova
    private Propriedade existente;        // preenchida no modo edição

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_propriedade);
        vm = new ViewModelProvider(this).get(PropriedadeViewModel.class);

        edtNome = findViewById(R.id.edtNome);
        edtLocalidade = findViewById(R.id.edtLocalidade);
        edtArea = findViewById(R.id.edtArea);
        edtTipoSolo = findViewById(R.id.edtTipoSolo);
        edtMatricula = findViewById(R.id.edtMatricula);
        edtCib = findViewById(R.id.edtCib);
        edtValor = findViewById(R.id.edtValor);
        edtObservacoes = findViewById(R.id.edtObservacoes);
        btnSalvar = findViewById(R.id.btnSalvar);

        propriedadeId = getIntent().getLongExtra(Extras.PROPRIEDADE_ID, 0L);
        setTitle(propriedadeId == 0L ? R.string.prop_nova_title : R.string.prop_editar_title);

        if (propriedadeId != 0L) {
            vm.obter(propriedadeId).observe(this, p -> {
                if (p != null && existente == null) { existente = p; preencher(p); }
            });
        }

        btnSalvar.setOnClickListener(v -> salvar());
        vm.getSalvoComId().observe(this, id -> {
            if (id == null) return;
            Toast.makeText(this, R.string.msg_prop_salva, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void preencher(Propriedade p) {
        edtNome.setText(p.nome);
        edtLocalidade.setText(p.localidade);
        edtArea.setText(p.areaTotal == null ? "" : String.valueOf(p.areaTotal));
        edtTipoSolo.setText(p.tipoSolo);
        edtMatricula.setText(p.matricula);
        edtCib.setText(p.cib);
        edtValor.setText(p.valor == null ? "" : String.valueOf(p.valor));
        edtObservacoes.setText(p.observacoes);
    }

    private void salvar() {
        String nome = txt(edtNome);
        if (nome.isEmpty()) {
            Toast.makeText(this, R.string.err_prop_nome_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }
        Propriedade p = (existente != null) ? existente : new Propriedade();
        p.id = propriedadeId;
        p.usuarioId = new SessionManager(this).getUsuarioId();
        p.nome = nome;
        p.localidade = nullSeVazio(txt(edtLocalidade));
        p.areaTotal = parseDouble(txt(edtArea));
        p.tipoSolo = nullSeVazio(txt(edtTipoSolo));
        p.matricula = nullSeVazio(txt(edtMatricula));
        p.cib = nullSeVazio(txt(edtCib));
        p.valor = parseDouble(txt(edtValor));
        p.observacoes = nullSeVazio(txt(edtObservacoes));
        vm.salvar(p);
    }

    private Double parseDouble(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private String nullSeVazio(String s) { return TextUtils.isEmpty(s) ? null : s; }

    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
