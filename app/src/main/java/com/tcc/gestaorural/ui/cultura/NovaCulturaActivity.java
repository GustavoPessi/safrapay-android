package com.tcc.gestaorural.ui.cultura;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Cultura;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;

public class NovaCulturaActivity extends BaseActivity {
    private CulturaViewModel vm;
    private TextInputEditText edtNome;
    private long propriedadeId, culturaId = 0L;
    private Cultura existente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_cultura);
        vm = new ViewModelProvider(this).get(CulturaViewModel.class);
        edtNome = findViewById(R.id.edtNome);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        propriedadeId = getIntent().getLongExtra(NavKeys.PROPRIEDADE_ID, 0L);
        culturaId = getIntent().getLongExtra(NavKeys.CULTURA_ID, 0L);
        setTitle(culturaId == 0L ? R.string.cultura_nova_title : R.string.cultura_editar_title);

        if (culturaId != 0L) {
            vm.obter(culturaId).observe(this, c -> {
                if (c != null && existente == null) { existente = c; edtNome.setText(c.nome); }
            });
        }

        btnSalvar.setOnClickListener(v -> salvar());
        vm.getSalvoComId().observe(this, id -> {
            if (id == null) return;
            Toast.makeText(this, R.string.msg_cultura_salva, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void salvar() {
        String nome = edtNome.getText() == null ? "" : edtNome.getText().toString().trim();
        if (nome.isEmpty()) { Toast.makeText(this, R.string.err_cultura_nome, Toast.LENGTH_LONG).show(); return; }
        Cultura c = (existente != null) ? existente : new Cultura();
        c.id = culturaId;
        c.propriedadeId = propriedadeId;
        c.nome = nome;
        vm.salvar(c);
    }
}
