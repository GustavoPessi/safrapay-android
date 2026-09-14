package com.tcc.gestaorural.ui.perfil;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Usuario;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.SessionManager;

public class EditarPerfilActivity extends BaseActivity {

    private UsuarioViewModel vm;
    private TextInputEditText edtNome, edtCpf, edtTelefone, edtNascimento, edtEmail;
    private Usuario usuario;
    private long usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);
        setTitle(R.string.editar_perfil_title);

        vm = new ViewModelProvider(this).get(UsuarioViewModel.class);
        edtNome = findViewById(R.id.edtNome);
        edtCpf = findViewById(R.id.edtCpf);
        edtTelefone = findViewById(R.id.edtTelefone);
        edtNascimento = findViewById(R.id.edtNascimento);
        edtEmail = findViewById(R.id.edtEmail);
        MaskUtils.aplicarCpf(edtCpf);
        MaskUtils.aplicarTelefone(edtTelefone);
        MaskUtils.aplicarData(edtNascimento);

        // E-mail nao e editavel aqui: mudar o e-mail de uma conta ja
        // verificada exigiria um fluxo de reconfirmacao que ainda nao
        // existe no backend. Mostramos o valor, so nao deixamos editar.
        edtEmail.setEnabled(false);

        usuarioId = new SessionManager(this).getUsuarioId();
        vm.obter(usuarioId).observe(this, u -> {
            if (u != null && usuario == null) {
                usuario = u;
                edtNome.setText(u.nome);
                edtCpf.setText(u.cpf == null ? "" : MaskUtils.formatarCpf(u.cpf));
                edtTelefone.setText(u.telefone);
                edtNascimento.setText(u.nascimento);
                edtEmail.setText(u.email);
            }
        });

        findViewById(R.id.btnSalvar).setOnClickListener(v -> salvar());
        vm.getResultado().observe(this, ev -> {
            if (ev == null) return;
            if (ev.ok) {
                Toast.makeText(this, R.string.msg_perfil_salvo, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, ev.erroResId, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void salvar() {
        if (usuario == null) return;
        String nome = txt(edtNome);
        if (nome.isEmpty()) { Toast.makeText(this, R.string.err_campos_obrigatorios, Toast.LENGTH_LONG).show(); return; }
        String cpf = txt(edtCpf);
        String cpfDigitos = MaskUtils.digitos(cpf);
        if (!cpfDigitos.isEmpty() && cpfDigitos.length() != 11) {
            Toast.makeText(this, R.string.err_cpf_invalido, Toast.LENGTH_LONG).show(); return;
        }
        usuario.nome = nome;
        usuario.cpf = cpfDigitos.isEmpty() ? null : cpfDigitos;
        usuario.telefone = txt(edtTelefone).isEmpty() ? null : txt(edtTelefone);
        usuario.nascimento = txt(edtNascimento).isEmpty() ? null : txt(edtNascimento);
        vm.salvarPerfil(usuario);
    }

    private String txt(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
}
