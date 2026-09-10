package com.tcc.gestaorural.ui.relatorio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.relatorio.RelatorioViewModel.MesLinha;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

public class RelatorioActivity extends BaseActivity {

    private static final String[] MESES_ABREV = {
        "jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"
    };

    private RelatorioViewModel vm;
    private TextInputEditText edtInicio, edtFim;
    private TextView txtTotReceitas, txtTotDespesas, txtTotSaldo, txtVazio;
    private LinearLayout chartContainer;
    private long usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relatorio);
        setTitle(R.string.relatorio_title);

        vm = new ViewModelProvider(this).get(RelatorioViewModel.class);
        usuarioId = new SessionManager(this).getUsuarioId();

        edtInicio = findViewById(R.id.edtInicio);
        edtFim = findViewById(R.id.edtFim);
        txtTotReceitas = findViewById(R.id.txtTotReceitas);
        txtTotDespesas = findViewById(R.id.txtTotDespesas);
        txtTotSaldo = findViewById(R.id.txtTotSaldo);
        txtVazio = findViewById(R.id.txtVazio);
        chartContainer = findViewById(R.id.chartContainer);

        MaskUtils.aplicarData(edtInicio);
        MaskUtils.aplicarData(edtFim);

        // Período padrão: ano corrente
        String ano = DateUtils.hojeIso().substring(0, 4);
        edtInicio.setText("01/01/" + ano);
        edtFim.setText(DateUtils.hojeBr());

        ((Button) findViewById(R.id.btnAplicar)).setOnClickListener(v -> aplicar());
        findViewById(R.id.btnExportar).setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_export_em_breve, Toast.LENGTH_SHORT).show());

        vm.getRelatorio().observe(this, r -> {
            if (r == null) return;
            txtTotReceitas.setText(MoneyUtils.formatar(r.totalReceitas));
            txtTotDespesas.setText(MoneyUtils.formatar(r.totalDespesas));
            txtTotSaldo.setText(MoneyUtils.formatar(r.saldo));
            renderizarGrafico(r.meses);
        });

        aplicar();
    }

    private void aplicar() {
        String ini = DateUtils.brParaIso(txt(edtInicio));
        String fim = DateUtils.brParaIso(txt(edtFim));
        if (ini == null) ini = DateUtils.hojeIso().substring(0, 4) + "-01-01";
        if (fim == null) fim = DateUtils.hojeIso();
        vm.definirPeriodo(usuarioId, ini, fim);
    }

    private void renderizarGrafico(java.util.List<MesLinha> meses) {
        chartContainer.removeAllViews();
        if (meses == null || meses.isEmpty()) {
            txtVazio.setVisibility(View.VISIBLE);
            return;
        }
        txtVazio.setVisibility(View.GONE);

        double max = 0;
        for (MesLinha m : meses) max = Math.max(max, Math.max(m.receita, m.despesa));
        if (max <= 0) max = 1;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (MesLinha m : meses) {
            View row = inflater.inflate(R.layout.row_relatorio_mes, chartContainer, false);
            ((TextView) row.findViewById(R.id.txtMes)).setText(rotuloMes(m.mes));
            aplicarBarra(row.findViewById(R.id.barReceita), row.findViewById(R.id.spacerReceita), m.receita / max);
            aplicarBarra(row.findViewById(R.id.barDespesa), row.findViewById(R.id.spacerDespesa), m.despesa / max);
            ((TextView) row.findViewById(R.id.txtValores)).setText(
                    MoneyUtils.formatar(m.receita) + "  ·  " + MoneyUtils.formatar(m.despesa));
            chartContainer.addView(row);
        }
    }

    private void aplicarBarra(View bar, View spacer, double fracao) {
        float f = (float) Math.max(0, Math.min(1, fracao));
        bar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f));
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1 - f));
    }

    private String rotuloMes(String yyyyMM) {
        try {
            int ano = Integer.parseInt(yyyyMM.substring(0, 4));
            int mes = Integer.parseInt(yyyyMM.substring(5, 7));
            return MESES_ABREV[mes - 1] + "/" + (ano % 100);
        } catch (Exception e) { return yyyyMM; }
    }

    private String txt(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
}
