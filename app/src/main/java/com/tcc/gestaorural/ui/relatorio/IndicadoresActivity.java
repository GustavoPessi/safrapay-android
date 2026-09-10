package com.tcc.gestaorural.ui.relatorio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.Locale;

public class IndicadoresActivity extends BaseActivity {

    private static final Locale BR = new Locale("pt", "BR");

    private IndicadoresViewModel vm;
    private TextView txtMargem, txtReceitaHa, txtCustoHa, txtMetas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indicadores);
        setTitle(R.string.indicadores_title);

        vm = new ViewModelProvider(this).get(IndicadoresViewModel.class);
        txtMargem = findViewById(R.id.txtMargem);
        txtReceitaHa = findViewById(R.id.txtReceitaHa);
        txtCustoHa = findViewById(R.id.txtCustoHa);
        txtMetas = findViewById(R.id.txtMetas);

        findViewById(R.id.btnRelatorio).setOnClickListener(v ->
                startActivity(new Intent(this, RelatorioActivity.class)));
        findViewById(R.id.btnDre).setOnClickListener(v ->
                startActivity(new Intent(this, DreActivity.class)));

        long usuarioId = new SessionManager(this).getUsuarioId();
        vm.iniciar(usuarioId);
        vm.getDados().observe(this, d -> {
            if (d == null) return;
            txtMargem.setText(String.format(BR, "%.1f%%", d.margem));
            txtReceitaHa.setText(getString(R.string.fmt_por_ha, MoneyUtils.formatar(d.receitaPorHa)));
            txtCustoHa.setText(getString(R.string.fmt_por_ha, MoneyUtils.formatar(d.custoPorHa)));
            txtMetas.setText(getString(R.string.fmt_metas_ativas, d.metasAtivas));
        });
    }
}
