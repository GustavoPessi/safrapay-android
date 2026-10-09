package com.tcc.gestaorural.ui.metas;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.MetaFinanceira;
import com.tcc.gestaorural.data.repository.SafraRepository;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;

public class AcompanhamentoMetaActivity extends BaseActivity {

    private MetaViewModel vm;
    private TextView txtDescricao, txtCultura, txtValores, txtPercentual, txtFalta, txtPrazo, txtEstimativa;
    private LinearProgressIndicator progresso;
    private TextInputEditText edtPrecoSaca;

    private long metaId;
    private MetaFinanceira metaAtual;
    private double valorAlvo = 0, valorAtual = 0, receitas = 0, despesas = 0;
    private boolean saldoConfigurado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acompanhamento_meta);
        setTitle(R.string.meta_detalhe_title);

        vm = new ViewModelProvider(this).get(MetaViewModel.class);
        txtDescricao = findViewById(R.id.txtDescricao);
        txtCultura = findViewById(R.id.txtPropriedade);
        txtValores = findViewById(R.id.txtValores);
        txtPercentual = findViewById(R.id.txtPercentual);
        txtFalta = findViewById(R.id.txtFalta);
        txtPrazo = findViewById(R.id.txtPrazo);
        txtEstimativa = findViewById(R.id.txtEstimativa);
        progresso = findViewById(R.id.progresso);
        edtPrecoSaca = findViewById(R.id.edtPrecoSaca);

        metaId = getIntent().getLongExtra(NavKeys.META_ID, 0L);

        vm.obter(metaId).observe(this, m -> {
            if (m == null) return;
            metaAtual = m;
            valorAlvo = m.valorAlvo;
            txtDescricao.setText(m.descricao);
            txtPrazo.setText(m.prazo == null ? "" :
                    getString(R.string.meta_prazo_label, DateUtils.isoParaBr(m.prazo)));
            new SafraRepository(this).obterResumo(m.safraId).observe(this, r -> {
                if (r != null) {
                    String cultura = (r.culturaNome == null || r.culturaNome.trim().isEmpty())
                            ? getString(R.string.safra_sem_cultura) : r.culturaNome;
                    txtCultura.setText(cultura + "  •  " + r.anoSafra);
                }
            });
            configurarSaldo(m);
            atualizarUI();
        });

        edtPrecoSaca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { atualizarEstimativa(); }
        });

        findViewById(R.id.btnEditar).setOnClickListener(v -> {
            Intent i = new Intent(this, NovaMetaActivity.class);
            i.putExtra(NavKeys.META_ID, metaId);
            startActivity(i);
        });
        findViewById(R.id.btnExcluir).setOnClickListener(v -> confirmarExclusao());
    }

    private void configurarSaldo(MetaFinanceira m) {
        if (saldoConfigurado) return;
        saldoConfigurado = true;
        vm.receitaTotalSafra(m.safraId).observe(this, t -> { receitas = nz(t); recalcular(); });
        vm.despesaTotalSafra(m.safraId).observe(this, t -> { despesas = nz(t); recalcular(); });
    }

    private double nz(Double d) { return d == null ? 0 : d; }

    /**
     * Resultado da safra = receitas − despesas, SEM esconder prejuízo: antes o negativo virava
     * R$ 0,00 e a meta parecia só "não começada" (relato do Gustavo, 09/10/2026).
     */
    private void recalcular() { valorAtual = receitas - despesas; atualizarUI(); }

    private void atualizarUI() {
        boolean prejuizo = valorAtual < 0;
        txtValores.setText(getString(R.string.meta_de,
                MoneyUtils.formatar(valorAtual), MoneyUtils.formatar(valorAlvo)));
        txtValores.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                prejuizo ? R.color.despesa : R.color.text_primary));
        int pct = (valorAlvo > 0) ? (int) Math.max(0, Math.min(100, Math.round(valorAtual / valorAlvo * 100))) : 0;
        if (prejuizo) {
            txtPercentual.setText(getString(R.string.meta_prejuizo, MoneyUtils.formatar(-valorAtual)));
            txtPercentual.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.despesa));
        } else {
            txtPercentual.setText(getString(R.string.meta_percentual, pct));
            txtPercentual.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.green_700));
        }
        progresso.setProgressCompat(pct, true);
        // Falta = alvo − resultado: com prejuízo, inclui cobrir o prejuízo antes de chegar na meta.
        double falta = Math.max(0, valorAlvo - valorAtual);
        if (falta <= 0) txtFalta.setText(R.string.meta_concluida);
        else if (prejuizo) txtFalta.setText(getString(R.string.meta_falta_com_prejuizo, MoneyUtils.formatar(falta)));
        else txtFalta.setText(getString(R.string.meta_falta, MoneyUtils.formatar(falta)));
        atualizarEstimativa();
    }

    private void atualizarEstimativa() {
        double falta = Math.max(0, valorAlvo - valorAtual);
        if (falta <= 0) { txtEstimativa.setText(R.string.meta_concluida); return; }
        Double preco = MoneyUtils.parse(edtPrecoSaca.getText() == null ? "" : edtPrecoSaca.getText().toString());
        if (preco == null || preco <= 0) { txtEstimativa.setText(R.string.meta_sacas_info); return; }
        long sacas = (long) Math.ceil(falta / preco);
        txtEstimativa.setText(getString(R.string.meta_sacas_result, String.valueOf(sacas), MoneyUtils.formatar(preco)));
    }

    private void confirmarExclusao() {
        if (metaAtual == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.acao_excluir)
                .setMessage(metaAtual.descricao)
                .setPositiveButton(R.string.acao_excluir, (d, w) -> {
                    vm.excluir(metaAtual);
                    Toast.makeText(this, R.string.msg_meta_excluida, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
