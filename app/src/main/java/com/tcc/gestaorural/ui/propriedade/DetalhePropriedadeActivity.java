package com.tcc.gestaorural.ui.propriedade;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.talhao.ListaTalhoesActivity;
import com.tcc.gestaorural.data.local.entity.Propriedade;

public class DetalhePropriedadeActivity extends BaseActivity {

    private PropriedadeViewModel vm;
    private TextView txtNome, txtLocalidade, txtArea, txtTipoSolo, txtMatricula, txtCib, txtValor, txtObservacoes;
    private long propriedadeId;
    private Propriedade atual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_propriedade);
        setTitle(R.string.prop_detalhe_title);

        vm = new ViewModelProvider(this).get(PropriedadeViewModel.class);
        txtNome = findViewById(R.id.txtNome);
        txtLocalidade = findViewById(R.id.txtLocalidade);
        txtArea = findViewById(R.id.txtArea);
        txtTipoSolo = findViewById(R.id.txtTipoSolo);
        txtMatricula = findViewById(R.id.txtMatricula);
        txtCib = findViewById(R.id.txtCib);
        txtValor = findViewById(R.id.txtValor);
        txtObservacoes = findViewById(R.id.txtObservacoes);

        propriedadeId = getIntent().getLongExtra(Extras.PROPRIEDADE_ID, 0L);

        vm.obter(propriedadeId).observe(this, p -> {
            if (p == null) return;
            atual = p;
            mostrar(p);
        });

        findViewById(R.id.btnEditar).setOnClickListener(v -> {
            Intent i = new Intent(this, NovaPropriedadeActivity.class);
            i.putExtra(Extras.PROPRIEDADE_ID, propriedadeId);
            startActivity(i);
        });

        findViewById(R.id.btnTalhoes).setOnClickListener(v -> {
            Intent i = new Intent(this, ListaTalhoesActivity.class);
            i.putExtra(NavKeys.PROPRIEDADE_ID, propriedadeId);
            startActivity(i);
        });

        findViewById(R.id.btnExcluir).setOnClickListener(v -> confirmarExclusao());
    }

    private void mostrar(Propriedade p) {
        txtNome.setText(p.nome);
        txtLocalidade.setText(TextUtils.isEmpty(p.localidade) ? "" : p.localidade);
        txtArea.setText(getString(R.string.hint_prop_area) + ": " +
                (p.areaTotal == null ? "—" : p.areaTotal));
        txtTipoSolo.setText(getString(R.string.hint_prop_tipo_solo) + ": " + ouTraco(p.tipoSolo));
        txtMatricula.setText(getString(R.string.hint_prop_matricula) + ": " + ouTraco(p.matricula));
        txtCib.setText(getString(R.string.hint_prop_cib) + ": " + ouTraco(p.cib));
        txtValor.setText(getString(R.string.hint_prop_valor) + ": " +
                (p.valor == null ? "—" : com.tcc.gestaorural.util.MoneyUtils.formatar(p.valor)));
        txtObservacoes.setText(getString(R.string.hint_prop_observacoes) + ": " + ouTraco(p.observacoes));
    }

    private String ouTraco(String s) { return TextUtils.isEmpty(s) ? "—" : s; }

    private void confirmarExclusao() {
        if (atual == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.acao_excluir)
                .setMessage(atual.nome)
                .setPositiveButton(R.string.acao_excluir, (d, w) -> {
                    vm.excluir(atual);
                    Toast.makeText(this, R.string.msg_prop_excluida, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
