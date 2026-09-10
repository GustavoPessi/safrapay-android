package com.tcc.gestaorural.ui.talhao;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Talhao;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.safra.ListaSafrasActivity;

public class DetalheTalhaoActivity extends BaseActivity {

    private TalhaoViewModel vm;
    private TextView txtNome, txtCultura, txtArea;
    private long talhaoId;
    private Talhao atual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_talhao);
        setTitle(R.string.talhao_detalhe_title);

        vm = new ViewModelProvider(this).get(TalhaoViewModel.class);
        txtNome = findViewById(R.id.txtNome);
        txtCultura = findViewById(R.id.txtCultura);
        txtArea = findViewById(R.id.txtArea);
        talhaoId = getIntent().getLongExtra(NavKeys.TALHAO_ID, 0L);

        vm.obter(talhaoId).observe(this, t -> {
            if (t == null) return;
            atual = t;
            txtNome.setText(t.nome);
            txtCultura.setText((t.cultura == null || t.cultura.trim().isEmpty())
                    ? getString(R.string.safra_sem_cultura) : t.cultura);
            txtArea.setText(t.areaHa == null ? "" :
                    getString(R.string.fmt_area_ha, String.valueOf(t.areaHa)));
        });

        findViewById(R.id.btnSafras).setOnClickListener(v -> {
            Intent i = new Intent(this, ListaSafrasActivity.class);
            i.putExtra(NavKeys.TALHAO_ID, talhaoId);
            startActivity(i);
        });
        findViewById(R.id.btnEditar).setOnClickListener(v -> {
            Intent i = new Intent(this, NovoTalhaoActivity.class);
            i.putExtra(NavKeys.TALHAO_ID, talhaoId);
            startActivity(i);
        });
        findViewById(R.id.btnExcluir).setOnClickListener(v -> {
            if (atual == null) return;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.acao_excluir)
                    .setMessage(atual.nome)
                    .setPositiveButton(R.string.acao_excluir, (d, w) -> {
                        vm.excluir(atual);
                        Toast.makeText(this, R.string.msg_talhao_excluido, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }
}
