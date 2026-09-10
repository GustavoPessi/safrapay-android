package com.tcc.gestaorural.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import com.tcc.gestaorural.ui.BaseActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;

/**
 * Recuperação de senha (stub de UI). O envio real de e-mail depende de backend,
 * ainda fora do escopo desta etapa — por ora só valida e confirma visualmente.
 */
public class RecuperarSenhaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_senha);
        setTitle(R.string.recuperar_title);

        TextInputEditText edtEmail = findViewById(R.id.edtEmail);
        Button btnEnviar = findViewById(R.id.btnEnviar);

        btnEnviar.setOnClickListener(v -> {
            String email = edtEmail.getText() == null ? "" : edtEmail.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, R.string.err_email_invalido, Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, R.string.msg_recuperacao_enviada, Toast.LENGTH_LONG).show();
            finish();
        });
    }
}
