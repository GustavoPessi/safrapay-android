package com.tcc.gestaorural.ui.equipe;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.remote.dto.MembroEquipeDto;
import com.tcc.gestaorural.data.repository.EquipeRepository;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

/** Tela 33 • Minha equipe (só o dono): convidar, ver status, trocar papel e remover. */
public class EquipeActivity extends BaseActivity {

    private EquipeRepository repo;
    private TextView txtEstado;
    private LinearLayout lista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipe);
        setTitle(R.string.equipe_title);

        // Só o dono gerencia a equipe (o servidor também barra com 403).
        if (!new SessionManager(this).ehDono()) { finish(); return; }

        repo = new EquipeRepository(this);
        txtEstado = findViewById(R.id.txtEstado);
        lista = findViewById(R.id.listaMembros);
        findViewById(R.id.btnConvidar).setOnClickListener(v -> abrirConvite());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repo != null) carregar();
    }

    private void carregar() {
        txtEstado.setVisibility(View.VISIBLE);
        txtEstado.setText(R.string.equipe_carregando);
        repo.listarEquipe(new EquipeRepository.ListaCallback<MembroEquipeDto>() {
            @Override public void onLista(List<MembroEquipeDto> itens) { mostrar(itens); }
            @Override public void onErro(String mensagem) {
                lista.setVisibility(View.GONE);
                txtEstado.setText(mensagem);
            }
        });
    }

    private void mostrar(List<MembroEquipeDto> itens) {
        lista.removeAllViews();
        // Os recusados não precisam de ação: vão pro fim da lista.
        List<MembroEquipeDto> ordenados = new ArrayList<>();
        for (MembroEquipeDto m : itens) if (!"RECUSADO".equalsIgnoreCase(m.getStatus())) ordenados.add(m);
        for (MembroEquipeDto m : itens) if ("RECUSADO".equalsIgnoreCase(m.getStatus())) ordenados.add(m);

        if (ordenados.isEmpty()) {
            lista.setVisibility(View.GONE);
            txtEstado.setVisibility(View.VISIBLE);
            txtEstado.setText(R.string.equipe_vazia);
            return;
        }
        txtEstado.setVisibility(View.GONE);
        lista.setVisibility(View.VISIBLE);
        LayoutInflater inf = getLayoutInflater();
        for (int i = 0; i < ordenados.size(); i++) {
            MembroEquipeDto m = ordenados.get(i);
            if (i > 0) {
                View div = new View(this);
                div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                div.setBackgroundColor(getResources().getColor(R.color.outline));
                lista.addView(div);
            }
            View item = inf.inflate(R.layout.item_membro_equipe, lista, false);
            boolean temNome = m.getNome() != null && !m.getNome().trim().isEmpty();
            ((TextView) item.findViewById(R.id.txtNome)).setText(temNome ? m.getNome() : m.getEmail());
            TextView email = item.findViewById(R.id.txtEmail);
            email.setText(m.getEmail());
            email.setVisibility(temNome ? View.VISIBLE : View.GONE);
            ((TextView) item.findViewById(R.id.txtStatus)).setText(textoStatus(m));
            ((TextView) item.findViewById(R.id.txtPapel)).setText(nomePapel(m.getPapel()));
            if (!"RECUSADO".equalsIgnoreCase(m.getStatus())) item.setOnClickListener(v -> opcoes(m));
            lista.addView(item);
        }
    }

    private String textoStatus(MembroEquipeDto m) {
        String st = m.getStatus() == null ? "" : m.getStatus().toUpperCase();
        String data = DateUtils.dataBrDeInstante(m.getCriadoEm());
        switch (st) {
            case "PENDENTE": return getString(R.string.equipe_status_pendente) + (data.isEmpty() ? "" : " • " + data);
            case "ACEITO":   return getString(R.string.equipe_status_aceito);
            case "RECUSADO": return getString(R.string.equipe_status_recusado);
            default:         return st;
        }
    }

    private String nomePapel(String papel) {
        return SessionManager.PAPEL_GERENTE.equalsIgnoreCase(papel)
                ? getString(R.string.equipe_papel_gerente) : getString(R.string.equipe_papel_colaborador);
    }

    /** Toque num membro: trocar o papel ou remover (pendente = cancelar convite). */
    private void opcoes(MembroEquipeDto m) {
        boolean gerente = SessionManager.PAPEL_GERENTE.equalsIgnoreCase(m.getPapel());
        String outroPapel = gerente ? SessionManager.PAPEL_COLABORADOR : SessionManager.PAPEL_GERENTE;
        boolean pendente = "PENDENTE".equalsIgnoreCase(m.getStatus());
        String quem = (m.getNome() != null && !m.getNome().trim().isEmpty()) ? m.getNome() : m.getEmail();
        String[] acoes = {
                getString(R.string.equipe_trocar_papel, nomePapel(outroPapel)),
                getString(pendente ? R.string.equipe_cancelar_convite : R.string.equipe_remover) };
        new AlertDialog.Builder(this)
                .setTitle(quem)
                .setItems(acoes, (d, qual) -> {
                    if (qual == 0) repo.trocarPapel(m.getUuid(), outroPapel, recarregar());
                    else if (pendente) repo.remover(m.getUuid(), recarregar());
                    else confirmarRemocao(m, quem);
                })
                .show();
    }

    private void confirmarRemocao(MembroEquipeDto m, String quem) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.equipe_remover)
                .setMessage(getString(R.string.equipe_remover_pergunta, quem))
                .setPositiveButton(R.string.equipe_remover, (d, w) -> repo.remover(m.getUuid(), recarregar()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private EquipeRepository.Callback recarregar() {
        return new EquipeRepository.Callback() {
            @Override public void onOk() {
                Toast.makeText(EquipeActivity.this, R.string.equipe_feito, Toast.LENGTH_SHORT).show();
                carregar();
            }
            @Override public void onErro(String mensagem) {
                Toast.makeText(EquipeActivity.this, mensagem, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void abrirConvite() {
        View v = getLayoutInflater().inflate(R.layout.dialog_convidar, null);
        TextInputLayout til = v.findViewById(R.id.tilEmail);
        TextInputEditText edt = v.findViewById(R.id.edtEmail);
        RadioGroup grupo = v.findViewById(R.id.grupoPapel);
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(R.string.equipe_convidar_titulo)
                .setView(v)
                .setPositiveButton(R.string.equipe_convidar_enviar, null) // validado abaixo (não fecha se inválido)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String email = edt.getText() == null ? "" : edt.getText().toString().trim().toLowerCase();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                til.setError(getString(R.string.err_email_invalido));
                return;
            }
            til.setError(null);
            String papel = grupo.getCheckedRadioButtonId() == R.id.rbGerente
                    ? SessionManager.PAPEL_GERENTE : SessionManager.PAPEL_COLABORADOR;
            b.setEnabled(false);
            repo.convidar(email, papel, new EquipeRepository.Callback() {
                @Override public void onOk() {
                    d.dismiss();
                    Toast.makeText(EquipeActivity.this, R.string.equipe_convite_enviado, Toast.LENGTH_LONG).show();
                    carregar();
                }
                @Override public void onErro(String mensagem) {
                    b.setEnabled(true);
                    til.setError(mensagem);
                }
            });
        }));
        d.show();
    }
}
