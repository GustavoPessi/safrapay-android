package com.tcc.gestaorural.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.dashboard.DashboardActivity;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.SessionManager;

public class CadastroActivity extends BaseActivity {

    private AuthViewModel vm;
    private TextInputEditText edtNome, edtEmail, edtCpf, edtTelefone, edtNascimento, edtSenha, edtConfirmar;
    private Button btnCadastrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);
        setTitle(R.string.cadastro_title);
        vm = new ViewModelProvider(this).get(AuthViewModel.class);

        edtNome = findViewById(R.id.edtNome);
        edtEmail = findViewById(R.id.edtEmail);
        edtCpf = findViewById(R.id.edtCpf);
        edtTelefone = findViewById(R.id.edtTelefone);
        edtNascimento = findViewById(R.id.edtNascimento);
        edtSenha = findViewById(R.id.edtSenha);
        edtConfirmar = findViewById(R.id.edtConfirmar);
        btnCadastrar = findViewById(R.id.btnCadastrar);

        // Máscaras padrão do projeto
        MaskUtils.aplicarCpf(edtCpf);
        MaskUtils.aplicarTelefone(edtTelefone);
        MaskUtils.aplicarData(edtNascimento);

        findViewById(R.id.btnIrLogin).setOnClickListener(v -> finish());
        btnCadastrar.setOnClickListener(v -> tentarCadastro());

        vm.getCarregando().observe(this, c -> btnCadastrar.setEnabled(!c));
        vm.getResultado().observe(this, res -> {
            if (res == null) return;
            if (res.sucesso) {
                new SessionManager(this).salvarLogin(res.usuarioId);
                Toast.makeText(this, R.string.msg_conta_criada, Toast.LENGTH_SHORT).show();
                Intent i = new Intent(this, DashboardActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            } else {
                Toast.makeText(this, res.erroResId, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void tentarCadastro() {
        String nome = txt(edtNome), email = txt(edtEmail), senha = txt(edtSenha),
               confirmar = txt(edtConfirmar);

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, R.string.err_campos_obrigatorios, Toast.LENGTH_LONG).show(); return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.err_email_invalido, Toast.LENGTH_LONG).show(); return;
        }
        if (senha.length() < 6) {
            Toast.makeText(this, R.string.err_senha_curta, Toast.LENGTH_LONG).show(); return;
        }
        if (!senha.equals(confirmar)) {
            Toast.makeText(this, R.string.err_senhas_diferentes, Toast.LENGTH_LONG).show(); return;
        }
        String cpf = txt(edtCpf);
        if (!cpf.isEmpty() && MaskUtils.digitos(cpf).length() != 11) {
            Toast.makeText(this, R.string.err_cpf_invalido, Toast.LENGTH_LONG).show(); return;
        }
        vm.cadastrar(nome, email, senha, cpf, txt(edtTelefone), txt(edtNascimento));
    }

    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
