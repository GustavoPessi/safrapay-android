package com.tcc.gestaorural.ui.relatorio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.export.ExportadorRelatorio;
import com.tcc.gestaorural.export.ExportadorRelatorio.Formato;
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
    // Período aplicado por último (ISO) - é o que vai pra exportação.
    private String periodoIni, periodoFim;
    private boolean exportando = false;

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
        findViewById(R.id.btnExportar).setOnClickListener(v -> escolherFormato());

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
        periodoIni = ini;
        periodoFim = fim;
        vm.definirPeriodo(usuarioId, ini, fim);
    }

    // ------------------------------------------------------------------
    // Exportação (PDF / Excel / CSV) - gera no celular, a partir do Room
    // ------------------------------------------------------------------

    private void escolherFormato() {
        if (exportando) return;
        final Formato[] formatos = {Formato.PDF, Formato.XLSX, Formato.CSV};
        String[] opcoes = {
            getString(R.string.exp_opcao_pdf),
            getString(R.string.exp_opcao_xlsx),
            getString(R.string.exp_opcao_csv)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.exp_titulo)
                .setItems(opcoes, (d, which) -> {
                    if (formatos[which] == Formato.CSV) avisarAntesDoCsv(); else exportar(formatos[which]);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * O CSV é dado puro (pra importar em outra ferramenta), então o aviso de relatório
     * gerencial não vai dentro do arquivo — aparece aqui antes de gerar e vai junto no texto
     * do compartilhamento (corpo do e-mail / legenda no WhatsApp).
     */
    private void avisarAntesDoCsv() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exp_csv_aviso_titulo)
                .setMessage(R.string.aviso_relatorio_gerencial)
                .setPositiveButton(R.string.exp_csv_aviso_ok, (d, w) -> exportar(Formato.CSV))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void exportar(Formato formato) {
        aplicar(); // garante que o arquivo sai com o período que está nos campos
        final String ini = periodoIni, fim = periodoFim;
        exportando = true;
        Toast.makeText(this, R.string.exp_gerando, Toast.LENGTH_SHORT).show();
        AppDatabase.IO.execute(() -> {
            try {
                java.io.File arquivo = ExportadorRelatorio.gerar(getApplicationContext(), usuarioId, ini, fim, formato);
                runOnUiThread(() -> {
                    exportando = false;
                    if (!isFinishing()) ExportadorRelatorio.compartilhar(this, arquivo, formato);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    exportando = false;
                    Toast.makeText(this, R.string.exp_erro, Toast.LENGTH_LONG).show();
                });
            }
        });
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
