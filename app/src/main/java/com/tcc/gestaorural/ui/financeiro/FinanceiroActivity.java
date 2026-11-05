package com.tcc.gestaorural.ui.financeiro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tcc.gestaorural.R;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.tcc.gestaorural.data.local.model.Lancamento;
import com.tcc.gestaorural.data.local.model.SafraOpcao;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.comum.SeletorPeriodo;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class FinanceiroActivity extends BaseActivity {

    private FinanceiroViewModel vm;
    private LancamentoAdapter adapter;
    private TextView txtVazio, txtReceitas, txtDespesas, txtSaldo;
    private long safraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financeiro);
        setTitle(R.string.financeiro_title);

        vm = new ViewModelProvider(this).get(FinanceiroViewModel.class);
        safraId = getIntent().getLongExtra(NavKeys.SAFRA_ID, 0L);

        txtVazio = findViewById(R.id.txtVazio);
        txtReceitas = findViewById(R.id.txtTotalReceitas);
        txtDespesas = findViewById(R.id.txtTotalDespesas);
        txtSaldo = findViewById(R.id.txtSaldo);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LancamentoAdapter(this::abrirDetalhe);
        recycler.setAdapter(adapter);

        // Filtro de período: Tudo (padrão) / Dia / Mês / Ano / Intervalo.
        SeletorPeriodo seletor = new SeletorPeriodo(this, findViewById(R.id.seletorPeriodo), true, vm.getPeriodo());
        seletor.setListener(vm::setPeriodo);

        FloatingActionButton fab = findViewById(R.id.fabNovo);
        fab.setOnClickListener(v -> escolherTipo());

        if (isGlobal()) {
            setTitle(R.string.financas_global_title);
            long usuarioId = new SessionManager(this).getUsuarioId();
            vm.iniciarGlobal(usuarioId);
            vm.opcoesSafra(usuarioId).observe(this, this::popularFiltroSafra);
        } else {
            // Tela de uma safra só: o filtro de safra não faz sentido.
            findViewById(R.id.tilFiltroSafra).setVisibility(View.GONE);
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

    /** Lista suspensa "Safra": Todas as safras + cada safra (Talhão • Cultura • 2026/2027). */
    private void popularFiltroSafra(List<SafraOpcao> lista) {
        final List<Long> ids = new ArrayList<>();
        List<String> rotulos = new ArrayList<>();
        ids.add(0L);
        rotulos.add(getString(R.string.financas_todas_safras));
        if (lista != null) for (SafraOpcao o : lista) { ids.add(o.id); rotulos.add(o.rotulo()); }

        MaterialAutoCompleteTextView drop = findViewById(R.id.dropFiltroSafra);
        drop.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rotulos));
        int sel = ids.indexOf(vm.getSafraFiltro());
        if (sel < 0) { sel = 0; vm.setSafraFiltro(0L); } // safra apagada/sumiu: volta pra "todas"
        drop.setText(rotulos.get(sel), false);
        drop.setOnItemClickListener((p, v, pos, id) -> vm.setSafraFiltro(ids.get(pos)));
    }

    private void escolherTipo() {
        String[] opcoes = { getString(R.string.tipo_receita), getString(R.string.tipo_despesa) };
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_escolher_tipo)
                .setItems(opcoes, (d, which) -> {
                    Class<?> alvo = (which == 0) ? NovaReceitaActivity.class : NovaDespesaActivity.class;
                    Intent i = new Intent(this, alvo);
                    // Em Finanças geral com uma safra filtrada, o lançamento novo já vem nela.
                    i.putExtra(NavKeys.SAFRA_ID, isGlobal() ? vm.getSafraFiltro() : safraId);
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
