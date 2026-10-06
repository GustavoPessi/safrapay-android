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
import com.tcc.gestaorural.data.sync.SyncManager;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.SessionManager;

public class PerfilActivity extends BaseActivity implements com.tcc.gestaorural.ui.equipe.EquipeSincronizador.TelaComum {

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
        findViewById(R.id.rowSincronizar).setOnClickListener(v -> sincronizar());
        findViewById(R.id.rowConfig).setOnClickListener(v ->
                startActivity(new Intent(this, ConfiguracoesActivity.class)));
        findViewById(R.id.rowNotif).setOnClickListener(v ->
                startActivity(new Intent(this, NotificacoesActivity.class)));
        findViewById(R.id.rowSair).setOnClickListener(v -> {
            new com.tcc.gestaorural.data.repository.AuthRepository(this).sair();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        findViewById(R.id.rowMinhaEquipe).setOnClickListener(v ->
                startActivity(new Intent(this, com.tcc.gestaorural.ui.equipe.EquipeActivity.class)));
        findViewById(R.id.rowConvites).setOnClickListener(v ->
                startActivity(new Intent(this, com.tcc.gestaorural.ui.equipe.ConvitesActivity.class)));
        findViewById(R.id.rowSairEquipe).setOnClickListener(v -> confirmarSairDaEquipe());

        long usuarioId = new SessionManager(this).getUsuarioId();
        AppDatabase.getInstance(this).usuarioDao()
                .observarPorId(usuarioId)
                .observe(this, this::preencher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarEquipe();
    }

    /**
     * Dono: "Minha equipe" (convidar/gerenciar). Gerente/colaborador: de quem é a equipe e
     * "Sair da equipe". O colaborador não tem cadastros, sincronização nem notificações
     * (o app dele é online, só máquinas e tarefas).
     */
    private void atualizarEquipe() {
        SessionManager s = new SessionManager(this);
        boolean dono = s.ehDono();
        boolean colaborador = s.ehColaborador();
        TextView info = findViewById(R.id.txtEquipeInfo);
        if (dono) {
            info.setVisibility(android.view.View.GONE);
        } else {
            info.setVisibility(android.view.View.VISIBLE);
            info.setText(getString(colaborador ? R.string.equipe_info_colaborador : R.string.equipe_info_gerente,
                    s.getDonoNome() != null ? s.getDonoNome() : "—"));
        }
        findViewById(R.id.rowMinhaEquipe).setVisibility(dono ? android.view.View.VISIBLE : android.view.View.GONE);
        int n = s.getConvitesPendentes();
        ((TextView) findViewById(R.id.rowConvites)).setText(n > 0
                ? getString(R.string.equipe_convites_recebidos_n, n) : getString(R.string.equipe_convites_recebidos));
        findViewById(R.id.divSairEquipe).setVisibility(dono ? android.view.View.GONE : android.view.View.VISIBLE);
        findViewById(R.id.rowSairEquipe).setVisibility(dono ? android.view.View.GONE : android.view.View.VISIBLE);

        int vis = colaborador ? android.view.View.GONE : android.view.View.VISIBLE;
        findViewById(R.id.txtLabelCadastros).setVisibility(vis);
        findViewById(R.id.cardCadastros).setVisibility(vis);
        findViewById(R.id.divNotif).setVisibility(vis);
        findViewById(R.id.rowNotif).setVisibility(vis);
    }

    private void confirmarSairDaEquipe() {
        SessionManager s = new SessionManager(this);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.equipe_sair)
                .setMessage(getString(R.string.equipe_sair_pergunta, s.getDonoNome() != null ? s.getDonoNome() : "—"))
                .setPositiveButton(R.string.equipe_sair, (d, w) -> sairDaEquipe())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void sairDaEquipe() {
        Toast.makeText(this, R.string.equipe_saindo, Toast.LENGTH_SHORT).show();
        new com.tcc.gestaorural.data.repository.EquipeRepository(this).sairDaEquipe(
                new com.tcc.gestaorural.data.repository.EquipeRepository.SituacaoCallback() {
                    @Override public void onSituacao(com.tcc.gestaorural.data.repository.EquipeRepository.Situacao sit) {
                        if (sit.getMensagemTroca() != null)
                            Toast.makeText(PerfilActivity.this, sit.getMensagemTroca(), Toast.LENGTH_LONG).show();
                        Intent i = new Intent(PerfilActivity.this,
                                com.tcc.gestaorural.ui.equipe.EquipeSincronizador.telaInicial(new SessionManager(PerfilActivity.this)));
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    }
                    @Override public void onErro(String mensagem) {
                        Toast.makeText(PerfilActivity.this, mensagem, Toast.LENGTH_LONG).show();
                    }
                });
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

    private boolean sincronizando = false;

    private void sincronizar() {
        if (sincronizando) return;
        sincronizando = true;
        Toast.makeText(this, R.string.sync_em_andamento, Toast.LENGTH_SHORT).show();
        new SyncManager(this).sincronizarTudo(new SyncManager.Callback() {
            @Override public void onSucesso() {
                sincronizando = false;
                Toast.makeText(PerfilActivity.this, R.string.sync_sucesso, Toast.LENGTH_SHORT).show();
            }
            @Override public void onErro(String mensagem) {
                sincronizando = false;
                Toast.makeText(PerfilActivity.this, mensagem, Toast.LENGTH_LONG).show();
            }
        });
    }

    // O colaborador não tem a navegação de baixo (Início/Finanças/Metas): volta pra tela dele.
    @Override protected boolean isTopLevel() { return !new SessionManager(this).ehColaborador(); }
    @Override protected int bottomNavItemId() { return new SessionManager(this).ehColaborador() ? 0 : R.id.nav_perfil; }
}
