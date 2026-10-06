package com.tcc.gestaorural.ui.equipe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.remote.dto.ConviteRecebidoDto;
import com.tcc.gestaorural.data.repository.EquipeRepository;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.List;

/** Tela 34 • Convites recebidos: aceitar (entra na fazenda do dono) ou recusar. */
public class ConvitesActivity extends BaseActivity implements EquipeSincronizador.TelaComum {

    private EquipeRepository repo;
    private TextView txtEstado;
    private LinearLayout lista;
    private boolean ocupado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convites);
        setTitle(R.string.convites_title);
        repo = new EquipeRepository(this);
        txtEstado = findViewById(R.id.txtEstado);
        lista = findViewById(R.id.listaConvites);
        carregar();
    }

    private void carregar() {
        txtEstado.setVisibility(View.VISIBLE);
        txtEstado.setText(R.string.equipe_carregando);
        lista.removeAllViews();
        repo.convitesRecebidos(new EquipeRepository.ListaCallback<ConviteRecebidoDto>() {
            @Override public void onLista(List<ConviteRecebidoDto> itens) {
                new SessionManager(ConvitesActivity.this).setConvitesPendentes(itens.size());
                mostrar(itens);
            }
            @Override public void onErro(String mensagem) { txtEstado.setText(mensagem); }
        });
    }

    private void mostrar(List<ConviteRecebidoDto> itens) {
        lista.removeAllViews();
        if (itens.isEmpty()) { txtEstado.setText(R.string.convites_vazio); return; }
        txtEstado.setVisibility(View.GONE);
        for (ConviteRecebidoDto c : itens) {
            View item = getLayoutInflater().inflate(R.layout.item_convite, lista, false);
            boolean gerente = SessionManager.PAPEL_GERENTE.equalsIgnoreCase(c.getPapel());
            String dono = c.getDonoNome() != null ? c.getDonoNome() : "Um produtor";
            ((TextView) item.findViewById(R.id.txtConvite)).setText(getString(R.string.convites_texto, dono,
                    getString(gerente ? R.string.equipe_papel_gerente : R.string.equipe_papel_colaborador).toLowerCase()));
            ((TextView) item.findViewById(R.id.txtDescricaoPapel)).setText(
                    gerente ? R.string.equipe_papel_gerente_desc : R.string.equipe_papel_colaborador_desc);
            ((TextView) item.findViewById(R.id.txtData)).setText(DateUtils.dataBrDeInstante(c.getCriadoEm()));
            Button aceitar = item.findViewById(R.id.btnAceitar);
            Button recusar = item.findViewById(R.id.btnRecusar);
            aceitar.setOnClickListener(v -> confirmarAceite(c, dono));
            recusar.setOnClickListener(v -> recusar(c));
            lista.addView(item);
        }
    }

    private void confirmarAceite(ConviteRecebidoDto c, String dono) {
        if (ocupado) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.convites_aceitar)
                .setMessage(getString(R.string.convites_aceitar_pergunta, dono))
                .setPositiveButton(R.string.convites_aceitar, (d, w) -> aceitar(c))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void aceitar(ConviteRecebidoDto c) {
        ocupado = true;
        Toast.makeText(this, R.string.convites_entrando, Toast.LENGTH_SHORT).show();
        repo.aceitarConvite(c.getUuid(), new EquipeRepository.SituacaoCallback() {
            @Override public void onSituacao(EquipeRepository.Situacao s) {
                ocupado = false;
                if (s.getMensagemTroca() != null)
                    Toast.makeText(ConvitesActivity.this, s.getMensagemTroca(), Toast.LENGTH_LONG).show();
                Intent i = new Intent(ConvitesActivity.this,
                        EquipeSincronizador.telaInicial(new SessionManager(ConvitesActivity.this)));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
            @Override public void onErro(String mensagem) {
                ocupado = false;
                // 409: já é de outra equipe / é dono com membros — a mensagem do servidor explica.
                new AlertDialog.Builder(ConvitesActivity.this)
                        .setMessage(mensagem)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });
    }

    private void recusar(ConviteRecebidoDto c) {
        if (ocupado) return;
        ocupado = true;
        repo.recusarConvite(c.getUuid(), new EquipeRepository.Callback() {
            @Override public void onOk() {
                ocupado = false;
                Toast.makeText(ConvitesActivity.this, R.string.convites_recusado, Toast.LENGTH_SHORT).show();
                carregar();
            }
            @Override public void onErro(String mensagem) {
                ocupado = false;
                Toast.makeText(ConvitesActivity.this, mensagem, Toast.LENGTH_LONG).show();
            }
        });
    }
}
