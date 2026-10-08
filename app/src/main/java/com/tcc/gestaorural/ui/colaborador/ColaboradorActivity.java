package com.tcc.gestaorural.ui.colaborador;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.remote.dto.MaquinaColaboradorDto;
import com.tcc.gestaorural.data.remote.dto.TarefaDto;
import com.tcc.gestaorural.data.repository.ColaboradorRepository;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.equipe.BannerConvite;
import com.tcc.gestaorural.ui.equipe.EquipeSincronizador;
import com.tcc.gestaorural.ui.perfil.PerfilActivity;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.List;

/**
 * Telas 35/36 • App do colaborador (online). Duas abas:
 * - Máquinas: contadores de tarefas pendentes/atrasadas, "Ver tarefas" e "Reportar problema";
 * - Tarefas: filtros (Pendentes, Atrasadas, Feitas, Todas), "Marcar como realizado" e "Reabrir".
 * O colaborador só vê as tarefas dele e as sem responsável (regra do servidor).
 */
public class ColaboradorActivity extends BaseActivity implements EquipeSincronizador.TelaDoColaborador, EquipeSincronizador.AtualizaComEquipe {

    private static final int ABA_MAQUINAS = 0;
    private static final int ABA_TAREFAS = 1;
    private static final String[] FILTROS = { "PENDENTES", "ATRASADAS", "FEITAS", "TODAS" };

    private ColaboradorRepository repo;
    private TabLayout abas;
    private View scrollFiltros;
    private ChipGroup chipsFiltro;
    private TextView txtFiltroMaquina, txtEstado;
    private LinearLayout lista;

    private int abaAtual = ABA_MAQUINAS;
    private String filtro = "PENDENTES";
    private String maquinaUuid, maquinaNome;
    private boolean ocupado;
    /** Evita que uma resposta antiga (aba trocada no meio) pinte a lista errada. */
    private int geracao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colaborador);
        setTitle(R.string.colab_title);

        repo = new ColaboradorRepository(this);
        abas = findViewById(R.id.abas);
        scrollFiltros = findViewById(R.id.scrollFiltros);
        chipsFiltro = findViewById(R.id.chipsFiltro);
        txtFiltroMaquina = findViewById(R.id.txtFiltroMaquina);
        txtEstado = findViewById(R.id.txtEstado);
        lista = findViewById(R.id.lista);

        findViewById(R.id.btnPerfil).setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));
        txtFiltroMaquina.setOnClickListener(v -> { maquinaUuid = null; maquinaNome = null; carregar(); });

        abas.addTab(abas.newTab().setText(R.string.colab_aba_maquinas));
        abas.addTab(abas.newTab().setText(R.string.colab_aba_tarefas));
        abas.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { abaAtual = tab.getPosition(); carregar(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { carregar(); }
        });

        int[] rotulos = { R.string.colab_filtro_pendentes, R.string.colab_filtro_atrasadas,
                R.string.colab_filtro_feitas, R.string.colab_filtro_todas };
        for (int i = 0; i < FILTROS.length; i++) {
            Chip c = new Chip(this);
            c.setText(rotulos[i]);
            c.setCheckable(true);
            c.setId(View.generateViewId());
            c.setTag(FILTROS[i]);
            c.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_100)));
            chipsFiltro.addView(c);
            if (i == 0) c.setChecked(true);
        }
        chipsFiltro.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = g.findViewById(ids.get(0));
            if (c != null && c.getTag() != null) { filtro = (String) c.getTag(); carregar(); }
        });

        long usuarioId = new SessionManager(this).getUsuarioId();
        AppDatabase.getInstance(this).usuarioDao().observarPorId(usuarioId).observe(this, u -> {
            String nome = (u == null || u.nome == null) ? "" : u.nome.split(" ")[0];
            ((TextView) findViewById(R.id.txtOla)).setText(getString(R.string.colab_ola, nome));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cabecalho();
        carregar();
        // A conferência do papel ao abrir o app é da SincronizacaoAutomatica (chama aoAtualizarEquipe).
    }

    @Override public void aoAtualizarEquipe() { cabecalho(); }

    private void cabecalho() {
        SessionManager s = new SessionManager(this);
        ((TextView) findViewById(R.id.txtFazenda)).setText(
                getString(R.string.colab_fazenda, s.getDonoNome() != null ? s.getDonoNome() : "—"));
        BannerConvite.atualizar(this);
    }

    private void carregar() {
        final int minha = ++geracao;
        boolean tarefas = abaAtual == ABA_TAREFAS;
        scrollFiltros.setVisibility(tarefas ? View.VISIBLE : View.GONE);
        txtFiltroMaquina.setVisibility(tarefas && maquinaUuid != null ? View.VISIBLE : View.GONE);
        if (maquinaNome != null) txtFiltroMaquina.setText(maquinaNome + "  ✕");
        mostrarEstado(getString(R.string.equipe_carregando));

        if (tarefas) {
            repo.tarefas(filtro, maquinaUuid, new ColaboradorRepository.ListaCallback<TarefaDto>() {
                @Override public void onLista(List<TarefaDto> itens) { if (minha == geracao) mostrarTarefas(itens); }
                @Override public void onErro(String mensagem, int codigo) { if (minha == geracao) erro(mensagem, codigo); }
            });
        } else {
            repo.maquinas(new ColaboradorRepository.ListaCallback<MaquinaColaboradorDto>() {
                @Override public void onLista(List<MaquinaColaboradorDto> itens) { if (minha == geracao) mostrarMaquinas(itens); }
                @Override public void onErro(String mensagem, int codigo) { if (minha == geracao) erro(mensagem, codigo); }
            });
        }
    }

    private void mostrarEstado(String texto) {
        lista.removeAllViews();
        txtEstado.setVisibility(View.VISIBLE);
        txtEstado.setText(texto);
    }

    private void erro(String mensagem, int codigo) {
        mostrarEstado(mensagem);
        // 403: o papel mudou (removido, virou gerente...). Confere e leva pra tela certa.
        if (codigo == 403) EquipeSincronizador.verificar(this, s -> cabecalho());
    }

    // --- Máquinas ---------------------------------------------------------

    private void mostrarMaquinas(List<MaquinaColaboradorDto> itens) {
        lista.removeAllViews();
        if (itens.isEmpty()) { mostrarEstado(getString(R.string.colab_sem_maquinas)); return; }
        txtEstado.setVisibility(View.GONE);
        LayoutInflater inf = getLayoutInflater();
        for (MaquinaColaboradorDto m : itens) {
            View v = inf.inflate(R.layout.item_maquina_colaborador, lista, false);
            ((TextView) v.findViewById(R.id.txtNome)).setText(m.getNome());
            StringBuilder sub = new StringBuilder();
            junta(sub, m.getTipo()); junta(sub, m.getMarca()); junta(sub, m.getModelo());
            TextView txtSub = v.findViewById(R.id.txtSub);
            txtSub.setText(sub.toString());
            txtSub.setVisibility(sub.length() == 0 ? View.GONE : View.VISIBLE);
            int pend = m.getTarefasPendentes() != null ? m.getTarefasPendentes() : 0;
            int atr = m.getTarefasAtrasadas() != null ? m.getTarefasAtrasadas() : 0;
            TextView cont = v.findViewById(R.id.txtContadores);
            cont.setText(getString(R.string.colab_contadores, pend, atr));
            cont.setTextColor(ContextCompat.getColor(this, atr > 0 ? R.color.despesa : R.color.text_secondary));
            v.findViewById(R.id.btnVerTarefas).setOnClickListener(b -> {
                maquinaUuid = m.getUuid();
                maquinaNome = m.getNome();
                TabLayout.Tab t = abas.getTabAt(ABA_TAREFAS);
                if (t != null) t.select();
            });
            v.findViewById(R.id.btnReportar).setOnClickListener(b -> reportar(m));
            lista.addView(v);
        }
    }

    private static void junta(StringBuilder sb, String v) {
        if (v == null || v.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append(" • ");
        sb.append(v.trim());
    }

    private void reportar(MaquinaColaboradorDto m) {
        View v = getLayoutInflater().inflate(R.layout.dialog_reportar, null);
        TextInputLayout til = v.findViewById(R.id.tilTexto);
        TextInputEditText edt = v.findViewById(R.id.edtTexto);
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.colab_reportar_titulo, m.getNome()))
                .setView(v)
                .setPositiveButton(R.string.colab_reportar_enviar, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String texto = edt.getText() == null ? "" : edt.getText().toString().trim();
            if (texto.isEmpty()) { til.setError(getString(R.string.err_anotacao_texto)); return; }
            b.setEnabled(false);
            repo.reportar(m.getUuid(), texto, new ColaboradorRepository.TarefaCallback() {
                @Override public void onOk(TarefaDto t) {
                    d.dismiss();
                    Toast.makeText(ColaboradorActivity.this, R.string.colab_reportado, Toast.LENGTH_LONG).show();
                    carregar();
                }
                @Override public void onErro(String mensagem, int codigo) {
                    b.setEnabled(true);
                    til.setError(mensagem);
                }
            });
        }));
        d.show();
    }

    // --- Tarefas ----------------------------------------------------------

    private void mostrarTarefas(List<TarefaDto> itens) {
        lista.removeAllViews();
        if (itens.isEmpty()) { mostrarEstado(getString(R.string.colab_sem_tarefas)); return; }
        txtEstado.setVisibility(View.GONE);
        LayoutInflater inf = getLayoutInflater();
        for (TarefaDto t : itens) {
            View v = inf.inflate(R.layout.item_tarefa, lista, false);
            ((TextView) v.findViewById(R.id.txtTexto)).setText(t.getTexto());

            boolean feita = Boolean.TRUE.equals(t.getConcluida());
            boolean atrasada = !feita && Boolean.TRUE.equals(t.getAtrasada());
            TextView status = v.findViewById(R.id.txtStatus);
            int cor = feita ? R.color.receita : atrasada ? R.color.despesa : R.color.amber_500;
            status.setText(feita ? R.string.tarefa_status_realizada
                    : atrasada ? R.string.tarefa_status_atrasada : R.string.tarefa_status_pendente);
            status.setTextColor(ContextCompat.getColor(this, cor));

            ((TextView) v.findViewById(R.id.txtMaquina)).setText("🚜 " + (t.getMaquinaNome() != null ? t.getMaquinaNome() : ""));
            ((TextView) v.findViewById(R.id.txtDetalhes)).setText(detalhes(t, feita));

            Button acao = v.findViewById(R.id.btnAcao);
            acao.setText(feita ? R.string.colab_reabrir : R.string.colab_marcar_feita);
            acao.setOnClickListener(b -> alternar(t, feita));
            lista.addView(v);
        }
    }

    private String detalhes(TarefaDto t, boolean feita) {
        StringBuilder sb = new StringBuilder();
        if (t.getPrazo() != null && !t.getPrazo().isEmpty()) sb.append(getString(R.string.tarefa_prazo_x, DateUtils.isoParaBr(t.getPrazo())));
        if (sb.length() > 0) sb.append(" • ");
        if (Boolean.TRUE.equals(t.getMinha())) sb.append(getString(R.string.colab_tarefa_minha));
        else if (t.getResponsavel() == null) sb.append(getString(R.string.colab_tarefa_livre));
        else sb.append(getString(R.string.tarefa_responsavel_x, t.getResponsavel().getNome()));
        // Quando foi criada/reportada (data e hora) e por quem (pedido do Gustavo, 08/10/2026).
        String criada = criadaLinha(t.getCriadoPorNome(), DateUtils.diaHoraDeInstante(t.getCriadoEm()));
        if (!criada.isEmpty()) sb.append("\n").append(criada);
        if (feita && t.getConcluidoPorNome() != null) {
            sb.append("\n").append(getString(R.string.tarefa_concluida_por, t.getConcluidoPorNome()));
            String quando = DateUtils.diaHoraDeInstante(t.getConcluidoEm());
            if (!quando.isEmpty()) sb.append(" em ").append(quando);
        }
        return sb.toString();
    }

    private String criadaLinha(String quem, String quando) {
        boolean temQuem = quem != null && !quem.trim().isEmpty();
        if (temQuem && !quando.isEmpty()) return getString(R.string.tarefa_criada_por_em, quem, quando);
        if (temQuem) return getString(R.string.tarefa_criada_por, quem);
        if (!quando.isEmpty()) return getString(R.string.tarefa_criada_em, quando);
        return "";
    }

    private void alternar(TarefaDto t, boolean feita) {
        if (ocupado) return;
        ocupado = true;
        ColaboradorRepository.TarefaCallback cb = new ColaboradorRepository.TarefaCallback() {
            @Override public void onOk(TarefaDto atualizada) { ocupado = false; carregar(); }
            @Override public void onErro(String mensagem, int codigo) {
                ocupado = false;
                Toast.makeText(ColaboradorActivity.this, mensagem, Toast.LENGTH_LONG).show();
                if (codigo == 403) EquipeSincronizador.verificar(ColaboradorActivity.this, s -> cabecalho());
            }
        };
        if (feita) repo.reabrir(t.getUuid(), cb); else repo.concluir(t.getUuid(), cb);
    }

    @Override protected boolean isTopLevel() { return true; }
}
