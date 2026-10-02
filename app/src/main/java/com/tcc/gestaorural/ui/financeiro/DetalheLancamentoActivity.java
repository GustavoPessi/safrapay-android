package com.tcc.gestaorural.ui.financeiro;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.tcc.gestaorural.ui.BaseActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Receita;
import com.tcc.gestaorural.data.local.model.Lancamento;
import com.tcc.gestaorural.data.local.entity.Anexo;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.comprovante.ComprovanteCampo;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;

public class DetalheLancamentoActivity extends BaseActivity {

    private ReceitaViewModel receitaVM;
    private DespesaViewModel despesaVM;

    private TextView txtValor, txtDescricao, txtTipo, txtData;
    private ImageView imgTipo;

    private long lancamentoId;
    private String tipo;
    private boolean receita;

    private Receita receitaAtual;
    private Despesa despesaAtual;
    private ComprovanteCampo comprovante;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_lancamento);
        setTitle(R.string.lancamento_detalhe_title);

        receitaVM = new ViewModelProvider(this).get(ReceitaViewModel.class);
        despesaVM = new ViewModelProvider(this).get(DespesaViewModel.class);

        imgTipo = findViewById(R.id.imgTipo);
        txtValor = findViewById(R.id.txtValor);
        txtDescricao = findViewById(R.id.txtDescricao);
        txtTipo = findViewById(R.id.txtTipo);
        txtData = findViewById(R.id.txtData);

        lancamentoId = getIntent().getLongExtra(NavKeys.LANCAMENTO_ID, 0L);
        tipo = getIntent().getStringExtra(NavKeys.LANCAMENTO_TIPO);
        receita = Lancamento.TIPO_RECEITA.equals(tipo);
        comprovante = new ComprovanteCampo(this, receita ? Anexo.TIPO_RECEITA : Anexo.TIPO_DESPESA, true);

        int cor = ContextCompat.getColor(this, receita ? R.color.receita : R.color.despesa);
        imgTipo.setImageResource(receita ? R.drawable.ic_receita : R.drawable.ic_despesa);
        txtValor.setTextColor(cor);
        txtTipo.setText(getString(R.string.label_tipo) + ": " +
                getString(receita ? R.string.tipo_receita : R.string.tipo_despesa));

        if (receita) {
            receitaVM.obter(lancamentoId).observe(this, r -> {
                if (r == null) return;
                receitaAtual = r;
                comprovante.setLancamentoUuid(r.uuid);
                preencher(r.descricao, r.valor, r.dataRegistro);
            });
        } else {
            despesaVM.obter(lancamentoId).observe(this, d -> {
                if (d == null) return;
                despesaAtual = d;
                comprovante.setLancamentoUuid(d.uuid);
                preencher(d.descricao, d.valor, d.dataRegistro);
            });
        }

        findViewById(R.id.btnEditar).setOnClickListener(v -> editar());
        findViewById(R.id.btnExcluir).setOnClickListener(v -> confirmarExclusao());
    }

    private void preencher(String descricao, double valor, String dataIso) {
        txtValor.setText(MoneyUtils.comSinal(valor, receita));
        txtDescricao.setText(descricao);
        txtData.setText(getString(R.string.label_data) + ": " + DateUtils.isoParaBr(dataIso));
        comprovante.setLegenda(descricao + " • " + MoneyUtils.formatar(valor) + " • " + DateUtils.isoParaBr(dataIso));
    }

    private void editar() {
        Class<?> alvo = receita ? NovaReceitaActivity.class : NovaDespesaActivity.class;
        Intent i = new Intent(this, alvo);
        i.putExtra(NavKeys.LANCAMENTO_ID, lancamentoId);
        startActivity(i);
    }

    private void confirmarExclusao() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.acao_excluir)
                .setMessage(receita
                        ? (receitaAtual != null ? receitaAtual.descricao : "")
                        : (despesaAtual != null ? despesaAtual.descricao : ""))
                .setPositiveButton(R.string.acao_excluir, (d, w) -> {
                    if (receita && receitaAtual != null) receitaVM.excluir(receitaAtual);
                    else if (!receita && despesaAtual != null) despesaVM.excluir(despesaAtual);
                    Toast.makeText(this, R.string.msg_lancamento_excluido, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
