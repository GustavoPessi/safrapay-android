package com.tcc.gestaorural.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import com.tcc.gestaorural.ui.BaseActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.remote.RetrofitClient;
import com.tcc.gestaorural.data.remote.dto.EsqueciSenhaRequestDto;
import com.tcc.gestaorural.data.remote.dto.MensagemResponseDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Recuperação de senha: pede o e-mail e chama o backend real
 * (POST /api/auth/esqueci-senha). O backend sempre responde com sucesso -
 * mesma mensagem exista ou não o e-mail cadastrado, pra não vazar quais
 * contas existem (enumeration). Se o e-mail existir, chega uma mensagem
 * com um link pra uma página web (fora do app) onde a pessoa escolhe a
 * nova senha - ver EmailService/redefinir-senha.html no backend.
 */
public class RecuperarSenhaActivity extends BaseActivity {

    private Button btnEnviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_senha);
        setTitle(R.string.recuperar_title);

        TextInputEditText edtEmail = findViewById(R.id.edtEmail);
        btnEnviar = findViewById(R.id.btnEnviar);

        btnEnviar.setOnClickListener(v -> {
            String email = edtEmail.getText() == null ? "" : edtEmail.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, R.string.err_email_invalido, Toast.LENGTH_LONG).show();
                return;
            }
            enviarSolicitacao(email);
        });
    }

    private void enviarSolicitacao(String email) {
        btnEnviar.setEnabled(false);

        RetrofitClient.getApi().esqueciSenha(new EsqueciSenhaRequestDto(email))
                .enqueue(new Callback<MensagemResponseDto>() {
                    @Override
                    public void onResponse(Call<MensagemResponseDto> call, Response<MensagemResponseDto> response) {
                        // Resposta de sucesso aqui não confirma que o e-mail existe -
                        // o backend responde igual nos dois casos de propósito.
                        Toast.makeText(RecuperarSenhaActivity.this,
                                R.string.msg_recuperacao_enviada, Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onFailure(Call<MensagemResponseDto> call, Throwable t) {
                        btnEnviar.setEnabled(true);
                        Toast.makeText(RecuperarSenhaActivity.this,
                                R.string.err_servidor, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
