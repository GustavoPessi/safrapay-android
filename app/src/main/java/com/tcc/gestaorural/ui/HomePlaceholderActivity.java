package com.tcc.gestaorural.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.ui.auth.LoginActivity;
import com.tcc.gestaorural.ui.perfil.PerfilActivity;
import com.tcc.gestaorural.ui.propriedade.ListaPropriedadesActivity;
import com.tcc.gestaorural.util.SessionManager;

/** Menu inicial (placeholder do Dashboard). Dá acesso às telas já implementadas. */
public class HomePlaceholderActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_placeholder);
        setTitle(R.string.app_name);

        final TextView txtOla = findViewById(R.id.txtOla);
        long usuarioId = new SessionManager(this).getUsuarioId();
        AppDatabase.getInstance(this).usuarioDao().observarPorId(usuarioId).observe(this, u -> {
            String nome = (u == null) ? "" : u.nome.split(" ")[0];
            txtOla.setText(getString(R.string.home_ola, nome));
        });

        findViewById(R.id.cardPropriedades).setOnClickListener(v ->
                startActivity(new Intent(this, ListaPropriedadesActivity.class)));

        findViewById(R.id.cardPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, PerfilActivity.class)));

        findViewById(R.id.btnSair).setOnClickListener(v -> {
            new SessionManager(this).logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    @Override
    protected boolean isTopLevel() { return true; }
}
