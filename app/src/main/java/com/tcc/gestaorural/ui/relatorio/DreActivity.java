package com.tcc.gestaorural.ui.relatorio;

import android.os.Bundle;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

public class DreActivity extends BaseActivity {

    private DreViewModel vm;
    private TextInputEditText edtAno;
    private TextView txtReceitas, txtImpostos, txtReceitaLiquida, txtCustos,
            txtResultadoBruto, txtOutras, txtResultado;
    private long usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dre);
        setTitle(R.string.dre_title);

        vm = new ViewModelProvider(this).get(DreViewModel.class);
        usuarioId = new SessionManager(this).getUsuarioId();

        edtAno = findViewById(R.id.edtAno);
        txtReceitas = findViewById(R.id.txtReceitas);
        txtImpostos = findViewById(R.id.txtImpostos);
        txtReceitaLiquida = findViewById(R.id.txtReceitaLiquida);
        txtCustos = findViewById(R.id.txtCustos);
        txtResultadoBruto = findViewById(R.id.txtResultadoBruto);
        txtOutras = findViewById(R.id.txtOutras);
        txtResultado = findViewById(R.id.txtResultado);

        edtAno.setText(DateUtils.hojeIso().substring(0, 4));
        findViewById(R.id.btnAplicar).setOnClickListener(v -> aplicar());

        vm.getDre().observe(this, d -> {
            if (d == null) return;
            txtReceitas.setText(MoneyUtils.formatar(d.receitas));
            txtImpostos.setText(MoneyUtils.formatar(d.impostos));
            txtReceitaLiquida.setText(MoneyUtils.formatar(d.receitaLiquida));
            txtCustos.setText(MoneyUtils.formatar(d.custos));
            txtResultadoBruto.setText(MoneyUtils.formatar(d.resultadoBruto));
            txtOutras.setText(MoneyUtils.formatar(d.outras));
            txtResultado.setText(MoneyUtils.formatar(d.resultado));
            int cor = ContextCompat.getColor(this, d.resultado >= 0 ? R.color.receita : R.color.despesa);
            txtResultado.setTextColor(cor);
        });

        aplicar();
    }

    private void aplicar() {
        int ano;
        try {
            ano = Integer.parseInt(edtAno.getText() == null ? "" : edtAno.getText().toString().trim());
        } catch (NumberFormatException e) {
            ano = Integer.parseInt(DateUtils.hojeIso().substring(0, 4));
        }
        vm.definirAno(usuarioId, ano);
    }
}
