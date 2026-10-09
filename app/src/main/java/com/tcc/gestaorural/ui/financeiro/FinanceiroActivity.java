package com.tcc.gestaorural.ui.financeiro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.model.Lancamento;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

public class FinanceiroActivity extends BaseActivity {

    private FinanceiroViewModel vm;
    private LancamentoAdapter adapter;
    private TextView txtVazio, txtReceitas, txtDespesas, txtSaldo, txtSafra;
    private long safraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financeiro);
        setTitle(R.string.financeiro_title);

        vm = new ViewModelProvider(this).get(FinanceiroViewModel.class);
        safraId = getIntent().getLongExtra(NavKeys.SAFRA_ID, 0L);

        txtVazio = findViewById(R.id.txtVazio);
        txtSafra = findViewById(R.id.txtSafra);
        txtReceitas = findViewById(R.id.txtTotalReceitas);
        txtDespesas = findViewById(R.id.txtTotalDespesas);
        txtSaldo = findViewById(R.id.txtSaldo);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LancamentoAdapter(this::abrirDetalhe);
        recycler.setAdapter(adapter);

        ChipGroup chips = findViewById(R.id.chipsFiltro);
        chips.setOnCheckedStateChangeListener((group, ids) -> {
            int f = (group.getCheckedChipId() == R.id.chipMes)
                    ? FinanceiroViewModel.FILTRO_MES : FinanceiroViewModel.FILTRO_TUDO;
            vm.setFiltro(f);
        });

        FloatingActionButton fab = findViewById(R.id.fabNovo);
        fab.setOnClickListener(v -> escolherTipo());

        if (isGlobal()) {
            setTitle(R.string.financas_global_title);
            txtSafra.setText(R.string.financas_todas_safras);
            long usuarioId = new SessionManager(this).getUsuarioId();
            vm.iniciarGlobal(usuarioId);
        } else {
            vm.iniciar(safraId);
        }
        vm.getExtrato().observe(this, e -> {
            if (e == null) return;
            adapter.submeter(e.itens);
            txtVazio.setVisibility(e.itens.isEmpty() ? View.VISIBLE : View.GONE);
            txtReceitas.setText(MoneyUtils.formatar(e.receitas));
            txtDespesas.setText(MoneyUtils.formatar(e.despesas));
            txtSaldo.setText(MoneyUtils.formatar(e.saldo));
            txtSaldo.setTextColor(androidx.core.content.ContextCompat.getColor(this,
                    e.saldo < 0 ? R.color.despesa : R.color.text_primary));
        });
    }

    private void escolherTipo() {
        String[] opcoes = { getString(R.string.tipo_receita), getString(R.string.tipo_despesa) };
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_escolher_tipo)
                .setItems(opcoes, (d, which) -> {
                    Class<?> alvo = (which == 0) ? NovaReceitaActivity.class : NovaDespesaActivity.class;
                    Intent i = new Intent(this, alvo);
                    i.putExtra(NavKeys.SAFRA_ID, safraId);
                    startActivity(i);
                })
                .show();
    }

    private void abrirDetalhe(Lancamento l) {
        Intent i = new Intent(this, DetalheLancamentoActivity.class);
        i.putExtra(NavKeys.LANCAMENTO_ID, l.id);
        i.putExtra(NavKeys.LANCAMENTO_TIPO, l.tipo);
        startActivity(i);
    }

    private boolean isGlobal() {
        return getIntent().getBooleanExtra(NavKeys.FINANCAS_GLOBAL, false);
    }

    @Override protected boolean isTopLevel() { return isGlobal(); }
    @Override protected int bottomNavItemId() { return isGlobal() ? R.id.nav_financas : 0; }
}
