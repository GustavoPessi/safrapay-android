package com.tcc.gestaorural.ui.maquina;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.AnotacaoMaquina;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.data.local.model.Lancamento;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.financeiro.DetalheLancamentoActivity;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.MoneyUtils;

import java.util.List;

public class DetalheMaquinaActivity extends BaseActivity {

    private MaquinaViewModel vm;
    private TextView txtNome, txtSubtitulo, txtGasto, txtExtra, txtSemDespesas, txtSemAnotacoes;
    private android.widget.LinearLayout containerDespesas, containerAnotacoes;
    private long maquinaId;
    private Maquina atual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_maquina);
        setTitle(R.string.maquina_detalhe_title);

        vm = new ViewModelProvider(this).get(MaquinaViewModel.class);
        txtNome = findViewById(R.id.txtNome);
        txtSubtitulo = findViewById(R.id.txtSubtitulo);
        txtGasto = findViewById(R.id.txtGasto);
        txtExtra = findViewById(R.id.txtExtra);
        txtSemDespesas = findViewById(R.id.txtSemDespesas);
        txtSemAnotacoes = findViewById(R.id.txtSemAnotacoes);
        containerDespesas = findViewById(R.id.containerDespesas);
        containerAnotacoes = findViewById(R.id.containerAnotacoes);

        maquinaId = getIntent().getLongExtra(NavKeys.MAQUINA_ID, 0L);

        vm.obter(maquinaId).observe(this, m -> { if (m != null) { atual = m; mostrar(m); } });
        vm.totalDespesas(maquinaId).observe(this, t ->
                txtGasto.setText(MoneyUtils.formatar(t == null ? 0 : t)));
        vm.despesas(maquinaId).observe(this, this::renderDespesas);
        vm.anotacoes(maquinaId).observe(this, this::renderAnotacoes);

        findViewById(R.id.btnNovaAnotacao).setOnClickListener(v -> dialogNovaAnotacao());
        findViewById(R.id.btnEditar).setOnClickListener(v -> {
            Intent i = new Intent(this, NovaMaquinaActivity.class);
            i.putExtra(NavKeys.MAQUINA_ID, maquinaId);
            startActivity(i);
        });
        findViewById(R.id.btnExcluir).setOnClickListener(v -> confirmarExclusao());
    }

    private void mostrar(Maquina m) {
        txtNome.setText(m.nome);
        StringBuilder sb = new StringBuilder();
        junta(sb, m.tipo); junta(sb, m.marca); junta(sb, m.modelo); junta(sb, m.ano);
        txtSubtitulo.setText(sb.toString());
        StringBuilder extra = new StringBuilder();
        if (m.identificacao != null && !m.identificacao.isEmpty())
            extra.append(getString(R.string.hint_maq_identificacao)).append(": ").append(m.identificacao);
        if (m.valor != null) {
            if (extra.length() > 0) extra.append("\n");
            extra.append(getString(R.string.hint_maq_valor)).append(": ").append(MoneyUtils.formatar(m.valor));
        }
        if (m.observacoes != null && !m.observacoes.isEmpty()) {
            if (extra.length() > 0) extra.append("\n");
            extra.append(m.observacoes);
        }
        txtExtra.setText(extra.toString());
        txtExtra.setVisibility(extra.length() == 0 ? View.GONE : View.VISIBLE);
    }

    private void junta(StringBuilder sb, String v) {
        if (v == null || v.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append(" • ");
        sb.append(v.trim());
    }

    private void renderDespesas(List<Despesa> lista) {
        containerDespesas.removeAllViews();
        boolean vazio = (lista == null || lista.isEmpty());
        txtSemDespesas.setVisibility(vazio ? View.VISIBLE : View.GONE);
        if (vazio) return;
        LayoutInflater inf = LayoutInflater.from(this);
        int cor = ContextCompat.getColor(this, R.color.despesa);
        for (Despesa d : lista) {
            View row = inf.inflate(R.layout.item_lancamento, containerDespesas, false);
            ((ImageView) row.findViewById(R.id.imgTipo)).setImageResource(R.drawable.ic_despesa);
            ((TextView) row.findViewById(R.id.txtDescricao)).setText(d.descricao);
            ((TextView) row.findViewById(R.id.txtData)).setText(DateUtils.isoParaBr(d.dataRegistro));
            TextView valor = row.findViewById(R.id.txtValor);
            valor.setText(MoneyUtils.comSinal(d.valor, false));
            valor.setTextColor(cor);
            row.setOnClickListener(v -> {
                Intent i = new Intent(this, DetalheLancamentoActivity.class);
                i.putExtra(NavKeys.LANCAMENTO_ID, d.id);
                i.putExtra(NavKeys.LANCAMENTO_TIPO, Lancamento.TIPO_DESPESA);
                startActivity(i);
            });
            containerDespesas.addView(row);
        }
    }

    private void renderAnotacoes(List<AnotacaoMaquina> lista) {
        containerAnotacoes.removeAllViews();
        boolean vazio = (lista == null || lista.isEmpty());
        txtSemAnotacoes.setVisibility(vazio ? View.VISIBLE : View.GONE);
        if (vazio) return;
        LayoutInflater inf = LayoutInflater.from(this);
        String hoje = DateUtils.hojeIso();
        for (AnotacaoMaquina a : lista) {
            View row = inf.inflate(R.layout.item_anotacao, containerAnotacoes, false);
            ((TextView) row.findViewById(R.id.txtTexto)).setText(a.texto);
            TextView data = row.findViewById(R.id.txtData);
            if (a.data != null && !a.data.isEmpty()) {
                data.setText(DateUtils.isoParaBr(a.data));
                data.setVisibility(View.VISIBLE);
            } else {
                data.setVisibility(View.GONE);
            }

            // Status: verde (concluída) / vermelho (atrasada) / amarelo (no prazo) / neutro (sem prazo)
            int corRes; int labelRes;
            if (a.concluida == 1) { corRes = R.color.receita; labelRes = R.string.anotacao_status_concluida; }
            else if (a.data == null || a.data.isEmpty()) { corRes = R.color.text_hint; labelRes = R.string.anotacao_status_sem_prazo; }
            else if (a.data.compareTo(hoje) < 0) { corRes = R.color.despesa; labelRes = R.string.anotacao_status_atrasada; }
            else { corRes = R.color.amber_500; labelRes = R.string.anotacao_status_no_prazo; }
            int cor = ContextCompat.getColor(this, corRes);
            row.findViewById(R.id.dotStatus).setBackgroundTintList(ColorStateList.valueOf(cor));
            TextView status = row.findViewById(R.id.txtStatus);
            status.setText(labelRes);
            status.setTextColor(cor);

            row.setOnClickListener(v -> menuAnotacao(a));
            containerAnotacoes.addView(row);
        }
    }

    private void menuAnotacao(AnotacaoMaquina a) {
        boolean concluida = a.concluida == 1;
        String[] opcoes = {
                getString(concluida ? R.string.anotacao_marcar_pendente : R.string.anotacao_marcar_realizada),
                getString(R.string.acao_excluir)
        };
        new AlertDialog.Builder(this)
                .setTitle(a.texto)
                .setItems(opcoes, (d, which) -> {
                    if (which == 0) {
                        vm.marcarAnotacao(a, !concluida);
                        Toast.makeText(this, R.string.msg_anotacao_atualizada, Toast.LENGTH_SHORT).show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.acao_excluir)
                                .setMessage(a.texto)
                                .setPositiveButton(R.string.acao_excluir, (dd, ww) -> vm.excluirAnotacao(a))
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    }
                })
                .show();
    }

    private void dialogNovaAnotacao() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_nova_anotacao, null);
        TextInputEditText edtTexto = view.findViewById(R.id.edtTexto);
        TextInputEditText edtData = view.findViewById(R.id.edtData);
        MaskUtils.aplicarData(edtData);
        new AlertDialog.Builder(this)
                .setTitle(R.string.maq_nova_anotacao)
                .setView(view)
                .setPositiveButton(R.string.acao_salvar, (d, w) -> {
                    String texto = edtTexto.getText() == null ? "" : edtTexto.getText().toString().trim();
                    if (texto.isEmpty()) { Toast.makeText(this, R.string.err_anotacao_texto, Toast.LENGTH_LONG).show(); return; }
                    AnotacaoMaquina a = new AnotacaoMaquina();
                    a.maquinaId = maquinaId;
                    a.texto = texto;
                    String dataBr = edtData.getText() == null ? "" : edtData.getText().toString().trim();
                    a.data = dataBr.isEmpty() ? null : DateUtils.brParaIso(dataBr);
                    vm.salvarAnotacao(a);
                    Toast.makeText(this, R.string.msg_anotacao_salva, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmarExclusao() {
        if (atual == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.acao_excluir)
                .setMessage(atual.nome)
                .setPositiveButton(R.string.acao_excluir, (d, w) -> {
                    vm.excluir(atual);
                    Toast.makeText(this, R.string.msg_maquina_excluida, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
