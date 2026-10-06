package com.tcc.gestaorural.ui.config;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.auth.LoginActivity;
import com.tcc.gestaorural.ui.perfil.UsuarioViewModel;
import com.tcc.gestaorural.util.SessionManager;
import com.tcc.gestaorural.util.SettingsManager;

import androidx.lifecycle.ViewModelProvider;

public class ConfiguracoesActivity extends BaseActivity implements com.tcc.gestaorural.ui.equipe.EquipeSincronizador.TelaComum {

    private SettingsManager settings;
    private UsuarioViewModel vm;
    private long usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);
        setTitle(R.string.config_title);

        settings = new SettingsManager(this);
        vm = new ViewModelProvider(this).get(UsuarioViewModel.class);
        usuarioId = new SessionManager(this).getUsuarioId();

        ligarSwitch(R.id.swNotif, "notif_push", true);
        ligarSwitch(R.id.swBackup, "backup_auto", true);

        // Modo escuro: além de salvar, aplica na hora (recria as telas com a paleta noturna).
        com.google.android.material.materialswitch.MaterialSwitch swModo = findViewById(R.id.swModoEscuro);
        swModo.setChecked(settings.get("modo_escuro", false));
        swModo.setOnCheckedChangeListener((b, checked) -> {
            settings.set("modo_escuro", checked);
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        findViewById(R.id.rowExportar).setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_export_dados_em_breve, Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowAlterarSenha).setOnClickListener(v -> dialogAlterarSenha());
        findViewById(R.id.rowExcluir).setOnClickListener(v -> dialogExcluir());
        findViewById(R.id.rowSobre).setOnClickListener(v ->
                startActivity(new Intent(this, com.tcc.gestaorural.ui.sobre.SobreActivity.class)));

        vm.getResultado().observe(this, ev -> {
            if (ev == null) return;
            if (ev.ok) Toast.makeText(this, R.string.msg_senha_alterada, Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, ev.erroResId, Toast.LENGTH_LONG).show();
        });
        vm.getExcluido().observe(this, excluido -> {
            if (excluido == null || !excluido) return;
            new com.tcc.gestaorural.data.repository.AuthRepository(this).sair();
            Toast.makeText(this, R.string.msg_conta_excluida, Toast.LENGTH_LONG).show();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    private void ligarSwitch(int id, String chave, boolean padrao) {
        MaterialSwitch sw = findViewById(id);
        sw.setChecked(settings.get(chave, padrao));
        sw.setOnCheckedChangeListener((b, checked) -> settings.set(chave, checked));
    }

    private void dialogAlterarSenha() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_alterar_senha, null);
        TextInputEditText edtSenhaAtual = view.findViewById(R.id.edtSenhaAtual);
        TextInputEditText edtSenha = view.findViewById(R.id.edtSenha);
        TextInputEditText edtConfirmar = view.findViewById(R.id.edtConfirmar);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_nova_senha)
                .setView(view)
                .setPositiveButton(R.string.acao_salvar, (d, w) -> {
                    String atual = valor(edtSenhaAtual);
                    String s1 = valor(edtSenha), s2 = valor(edtConfirmar);
                    if (atual.isEmpty()) { Toast.makeText(this, R.string.hint_senha_atual, Toast.LENGTH_LONG).show(); return; }
                    if (s1.length() < 6) { Toast.makeText(this, R.string.err_senha_curta, Toast.LENGTH_LONG).show(); return; }
                    if (!s1.equals(s2)) { Toast.makeText(this, R.string.err_senhas_diferentes, Toast.LENGTH_LONG).show(); return; }
                    vm.alterarSenha(usuarioId, atual, s1);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void dialogExcluir() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.config_excluir_conta)
                .setMessage(R.string.dialog_confirmar_exclusao)
                .setPositiveButton(R.string.config_excluir_conta, (d, w) -> vm.excluirConta(usuarioId))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String valor(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString(); }
}
