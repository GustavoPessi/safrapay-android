package com.tcc.gestaorural.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.dashboard.DashboardActivity;
import com.tcc.gestaorural.util.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel vm;
    private TextInputLayout tilEmail, tilSenha;
    private TextInputEditText edtEmail, edtSenha;
    private Button btnEntrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        vm = new ViewModelProvider(this).get(AuthViewModel.class);

        tilEmail = findViewById(R.id.tilEmail);
        tilSenha = findViewById(R.id.tilSenha);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        btnEntrar = findViewById(R.id.btnEntrar);

        findViewById(R.id.btnIrCadastro).setOnClickListener(v ->
                startActivity(new Intent(this, CadastroActivity.class)));
        findViewById(R.id.btnEsqueci).setOnClickListener(v ->
                startActivity(new Intent(this, RecuperarSenhaActivity.class)));

        btnEntrar.setOnClickListener(v -> tentarLogin());

        vm.getCarregando().observe(this, carregando -> btnEntrar.setEnabled(!carregando));
        vm.getResultado().observe(this, res -> {
            if (res == null) return;
            if (res.sucesso) {
                new SessionManager(this).salvarLogin(res.usuarioId);
                Intent i = new Intent(this, DashboardActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            } else {
                Toast.makeText(this, res.erroResId, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void tentarLogin() {
        tilEmail.setError(null); tilSenha.setError(null);
        String email = txt(edtEmail), senha = txt(edtSenha);
        boolean ok = true;
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.err_email_invalido)); ok = false;
        }
        if (senha.isEmpty()) {
            tilSenha.setError(getString(R.string.err_campos_obrigatorios)); ok = false;
        }
        if (ok) vm.login(email, senha);
    }

    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
