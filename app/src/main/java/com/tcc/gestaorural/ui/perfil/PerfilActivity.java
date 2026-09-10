package com.tcc.gestaorural.ui.perfil;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.tcc.gestaorural.ui.BaseActivity;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import android.content.Intent;
import com.tcc.gestaorural.data.local.entity.Usuario;
import com.tcc.gestaorural.ui.config.ConfiguracoesActivity;
import com.tcc.gestaorural.ui.maquina.ListaMaquinasActivity;
import com.tcc.gestaorural.ui.propriedade.ListaPropriedadesActivity;
import com.tcc.gestaorural.ui.notificacoes.NotificacoesActivity;
import com.tcc.gestaorural.ui.auth.LoginActivity;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.SessionManager;

public class PerfilActivity extends BaseActivity {

    private TextView txtNome, txtEmail, txtCpf, txtTelefone, txtNascimento;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        setTitle(R.string.perfil_title);

        txtNome = findViewById(R.id.txtNome);
        txtEmail = findViewById(R.id.txtEmail);
        txtCpf = findViewById(R.id.txtCpf);
        txtTelefone = findViewById(R.id.txtTelefone);
        txtNascimento = findViewById(R.id.txtNascimento);

        findViewById(R.id.btnEditar).setOnClickListener(v ->
                startActivity(new Intent(this, EditarPerfilActivity.class)));
        findViewById(R.id.rowPropriedades).setOnClickListener(v ->
                startActivity(new Intent(this, ListaPropriedadesActivity.class)));
        findViewById(R.id.rowMaquinas).setOnClickListener(v ->
                startActivity(new Intent(this, ListaMaquinasActivity.class)));
        findViewById(R.id.rowConfig).setOnClickListener(v ->
                startActivity(new Intent(this, ConfiguracoesActivity.class)));
        findViewById(R.id.rowNotif).setOnClickListener(v ->
                startActivity(new Intent(this, NotificacoesActivity.class)));
        findViewById(R.id.rowSair).setOnClickListener(v -> {
            new SessionManager(this).logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        long usuarioId = new SessionManager(this).getUsuarioId();
        AppDatabase.getInstance(this).usuarioDao()
                .observarPorId(usuarioId)
                .observe(this, this::preencher);
    }

    private void preencher(Usuario u) {
        if (u == null) return;
        txtNome.setText(u.nome);
        txtEmail.setText(u.email);
        txtCpf.setText(ouTraco(u.cpf == null ? null : MaskUtils.formatarCpf(u.cpf)));
        txtTelefone.setText(ouTraco(u.telefone));
        txtNascimento.setText(ouTraco(u.nascimento));
    }

    private String ouTraco(String s) {
        return TextUtils.isEmpty(s) ? getString(R.string.perfil_sem_valor) : s;
    }

    @Override protected boolean isTopLevel() { return true; }
    @Override protected int bottomNavItemId() { return R.id.nav_perfil; }
}
