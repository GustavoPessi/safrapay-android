package com.tcc.gestaorural.ui.talhao;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Talhao;
import com.tcc.gestaorural.ui.NavKeys;

public class NovoTalhaoActivity extends BaseActivity {

    private TalhaoViewModel vm;
    private TextInputEditText edtNome, edtCultura, edtArea;
    private long talhaoId = 0L, propriedadeId = 0L;
    private Talhao existente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novo_talhao);
        vm = new ViewModelProvider(this).get(TalhaoViewModel.class);

        edtNome = findViewById(R.id.edtNome);
        edtCultura = findViewById(R.id.edtCultura);
        edtArea = findViewById(R.id.edtArea);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        propriedadeId = getIntent().getLongExtra(NavKeys.PROPRIEDADE_ID, 0L);
        talhaoId = getIntent().getLongExtra(NavKeys.TALHAO_ID, 0L);
        setTitle(talhaoId == 0L ? R.string.talhao_novo_title : R.string.talhao_editar_title);

        if (talhaoId != 0L) {
            vm.obter(talhaoId).observe(this, t -> {
                if (t != null && existente == null) {
                    existente = t;
                    propriedadeId = t.propriedadeId;
                    edtNome.setText(t.nome);
                    edtCultura.setText(t.cultura);
                    edtArea.setText(t.areaHa == null ? "" : String.valueOf(t.areaHa));
                }
            });
        }

        btnSalvar.setOnClickListener(v -> salvar());
        vm.getSalvoComId().observe(this, id -> {
            if (id == null) return;
            Toast.makeText(this, R.string.msg_talhao_salvo, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void salvar() {
        String nome = txt(edtNome);
        if (nome.isEmpty()) {
            Toast.makeText(this, R.string.err_talhao_nome_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }
        Talhao t = (existente != null) ? existente : new Talhao();
        t.id = talhaoId;
        t.propriedadeId = propriedadeId;
        t.nome = nome;
        String cultura = txt(edtCultura);
        t.cultura = TextUtils.isEmpty(cultura) ? null : cultura;
        String area = txt(edtArea);
        t.areaHa = TextUtils.isEmpty(area) ? null : safeDouble(area);
        vm.salvar(t);
    }

    private Double safeDouble(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
