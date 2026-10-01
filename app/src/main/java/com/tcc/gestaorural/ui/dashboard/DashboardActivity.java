package com.tcc.gestaorural.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.financeiro.NovaDespesaActivity;
import com.tcc.gestaorural.ui.financeiro.NovaReceitaActivity;
import com.tcc.gestaorural.ui.relatorio.IndicadoresActivity;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

public class DashboardActivity extends BaseActivity {

    private DashboardViewModel vm;
    private TextView txtOla, txtSaldo, txtReceitas, txtDespesas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        setTitle(R.string.app_name);

        vm = new ViewModelProvider(this).get(DashboardViewModel.class);
        txtOla = findViewById(R.id.txtOla);
        txtSaldo = findViewById(R.id.txtSaldo);
        txtReceitas = findViewById(R.id.txtReceitas);
        txtDespesas = findViewById(R.id.txtDespesas);

        long usuarioId = new SessionManager(this).getUsuarioId();

        AppDatabase.getInstance(this).usuarioDao().observarPorId(usuarioId).observe(this, u -> {
            String nome = (u == null) ? "" : u.nome.split(" ")[0];
            txtOla.setText(getString(R.string.home_ola, nome) + " 👋");
        });

        vm.iniciar(usuarioId);
        vm.getResumo().observe(this, r -> {
            if (r == null) return;
            txtSaldo.setText(MoneyUtils.formatar(r.saldo));
            txtReceitas.setText(MoneyUtils.formatar(r.receitas));
            txtDespesas.setText(MoneyUtils.formatar(r.despesas));
        });

        Button btnR = findViewById(R.id.btnAddReceita);
        Button btnD = findViewById(R.id.btnAddDespesa);
        btnR.setOnClickListener(v -> startActivity(new Intent(this, NovaReceitaActivity.class)));
        btnD.setOnClickListener(v -> startActivity(new Intent(this, NovaDespesaActivity.class)));
        findViewById(R.id.btnIndicadores).setOnClickListener(v ->
                startActivity(new Intent(this, IndicadoresActivity.class)));
        findViewById(R.id.btnNovaSafraRapida).setOnClickListener(v ->
                startActivity(new Intent(this, com.tcc.gestaorural.ui.safra.NovaSafraRapidaActivity.class)));
        findViewById(R.id.btnAssistente).setOnClickListener(v ->
                startActivity(new Intent(this, com.tcc.gestaorural.ui.chat.ChatIaActivity.class)));
    }

    @Override protected boolean isTopLevel() { return true; }
    @Override protected int bottomNavItemId() { return R.id.nav_inicio; }
}
