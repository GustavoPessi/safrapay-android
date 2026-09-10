package com.tcc.gestaorural.ui.sucesso;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.dashboard.DashboardActivity;

/** Tela de sucesso genérica (pós-salvamento). Sem barra/voltar: encerra no "Voltar ao início". */
public class SucessoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sucesso);

        String msg = getIntent().getStringExtra(NavKeys.SUCESSO_MSG);
        ((TextView) findViewById(R.id.txtMensagem)).setText(msg == null ? "" : msg);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> {
            Intent i = new Intent(this, DashboardActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}
