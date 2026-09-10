package com.tcc.gestaorural.ui.financeiro;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Receita;
import com.tcc.gestaorural.data.local.model.SafraOpcao;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.sucesso.SucessoActivity;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class NovaReceitaActivity extends BaseActivity {

    private ReceitaViewModel vm;
    private TextInputEditText edtDescricao, edtValor, edtData;
    private MaterialAutoCompleteTextView dropSafra;

    private final List<SafraOpcao> opcoes = new ArrayList<>();
    private long safraSelecionada = 0L;
    private long receitaId = 0L;
    private Receita existente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_receita);
        vm = new ViewModelProvider(this).get(ReceitaViewModel.class);

        edtDescricao = findViewById(R.id.edtDescricao);
        edtValor = findViewById(R.id.edtValor);
        edtData = findViewById(R.id.edtData);
        dropSafra = findViewById(R.id.dropSafra);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        MaskUtils.aplicarData(edtData);

        safraSelecionada = getIntent().getLongExtra(NavKeys.SAFRA_ID, 0L);
        receitaId = getIntent().getLongExtra(NavKeys.LANCAMENTO_ID, 0L);
        setTitle(receitaId == 0L ? R.string.nova_receita_title : R.string.editar_receita_title);

        if (receitaId == 0L) edtData.setText(DateUtils.hojeBr());

        long usuarioId = new SessionManager(this).getUsuarioId();
        vm.opcoesSafra(usuarioId).observe(this, this::popularSafras);

        if (receitaId != 0L) {
            vm.obter(receitaId).observe(this, r -> {
                if (r != null && existente == null) {
                    existente = r;
                    safraSelecionada = r.safraId;
                    edtDescricao.setText(r.descricao);
                    edtValor.setText(String.valueOf(r.valor));
                    edtData.setText(DateUtils.isoParaBr(r.dataRegistro));
                    marcarSafraSelecionada();
                }
            });
        }

        dropSafra.setOnItemClickListener((parent, view, position, id) -> {
            String sel = parent.getItemAtPosition(position).toString();
            for (SafraOpcao o : opcoes) if (o.rotulo().equals(sel)) { safraSelecionada = o.id; return; }
        });

        btnSalvar.setOnClickListener(v -> salvar());
        vm.getSalvoComId().observe(this, id -> {
            if (id == null) return;
            android.content.Intent i = new android.content.Intent(this, SucessoActivity.class);
            i.putExtra(NavKeys.SUCESSO_MSG, getString(R.string.sucesso_receita));
            startActivity(i);
            finish();
        });
    }

    private void popularSafras(List<SafraOpcao> lista) {
        opcoes.clear();
        if (lista != null) opcoes.addAll(lista);
        List<String> rotulos = new ArrayList<>();
        for (SafraOpcao o : opcoes) rotulos.add(o.rotulo());
        dropSafra.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rotulos));
        marcarSafraSelecionada();
    }

    private void marcarSafraSelecionada() {
        for (int i = 0; i < opcoes.size(); i++) {
            if (opcoes.get(i).id == safraSelecionada) {
                dropSafra.setText(opcoes.get(i).rotulo(), false);
                return;
            }
        }
    }

    private void salvar() {
        String desc = txt(edtDescricao);
        if (desc.isEmpty()) { toast(R.string.err_descricao_obrigatoria); return; }
        Double valor = MoneyUtils.parse(txt(edtValor));
        if (valor == null || valor <= 0) { toast(R.string.err_valor_obrigatorio); return; }
        if (safraSelecionada == 0L) { toast(R.string.err_safra_obrigatoria); return; }

        Receita r = (existente != null) ? existente : new Receita();
        r.id = receitaId;
        r.safraId = safraSelecionada;
        r.descricao = desc;
        r.valor = valor;
        String iso = DateUtils.brParaIso(txt(edtData));
        r.dataRegistro = (iso != null) ? iso : DateUtils.hojeIso();
        vm.salvar(r);
    }

    private void toast(int res) { Toast.makeText(this, res, Toast.LENGTH_LONG).show(); }
    private String txt(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
}
