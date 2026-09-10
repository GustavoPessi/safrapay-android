package com.tcc.gestaorural.ui.safra;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.financeiro.FinanceiroActivity;

public class DetalheSafraActivity extends BaseActivity {

    private SafraViewModel vm;
    private TextView txtCultura, txtAno, txtInicio, txtFim;
    private long safraId;
    private Safra atual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_safra);
        setTitle(R.string.safra_detalhe_title);

        vm = new ViewModelProvider(this).get(SafraViewModel.class);
        txtCultura = findViewById(R.id.txtCultura);
        txtAno = findViewById(R.id.txtAno);
        txtInicio = findViewById(R.id.txtInicio);
        txtFim = findViewById(R.id.txtFim);
        safraId = getIntent().getLongExtra(NavKeys.SAFRA_ID, 0L);

        vm.obterResumo(safraId).observe(this, r -> {
            if (r == null) return;
            txtCultura.setText(r.culturaNome);
            txtAno.setText((r.variedade == null || r.variedade.isEmpty())
                    ? r.anoSafra : r.anoSafra + "  •  " + r.variedade);
            txtInicio.setText(getString(R.string.hint_safra_inicio) + ": " + ouTraco(r.dataInicio));
            txtFim.setText(getString(R.string.hint_safra_fim) + ": " + ouTraco(r.dataFim));
        });
        // entidade para exclusão
        vm.obter(safraId).observe(this, s -> { if (s != null) atual = s; });

        findViewById(R.id.btnFinanceiro).setOnClickListener(v -> {
            Intent i = new Intent(this, FinanceiroActivity.class);
            i.putExtra(NavKeys.SAFRA_ID, safraId);
            startActivity(i);
        });
        findViewById(R.id.btnEditar).setOnClickListener(v -> {
            Intent i = new Intent(this, NovaSafraActivity.class);
            i.putExtra(NavKeys.SAFRA_ID, safraId);
            startActivity(i);
        });
        findViewById(R.id.btnExcluir).setOnClickListener(v -> {
            if (atual == null) return;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.acao_excluir)
                    .setMessage(txtCultura.getText())
                    .setPositiveButton(R.string.acao_excluir, (d, w) -> {
                        vm.excluir(atual);
                        Toast.makeText(this, R.string.msg_safra_excluida, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private String ouTraco(String s) { return TextUtils.isEmpty(s) ? "—" : s; }
}
