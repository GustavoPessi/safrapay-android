package com.tcc.gestaorural.ui.maquina;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.AnotacaoMaquina;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.data.local.model.Lancamento;
import com.tcc.gestaorural.data.remote.dto.EquipeMinhaResponseDto;
import com.tcc.gestaorural.data.repository.EquipeRepository;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.financeiro.DetalheLancamentoActivity;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class DetalheMaquinaActivity extends BaseActivity {

    private MaquinaViewModel vm;
    private TextView txtNome, txtSubtitulo, txtGasto, txtExtra, txtSemDespesas, txtSemAnotacoes;
    private android.widget.LinearLayout containerDespesas, containerAnotacoes;
    private long maquinaId;
    private Maquina atual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_maquina);
        setTitle(R.string.maquina_detalhe_title);

        vm = new ViewModelProvider(this).get(MaquinaViewModel.class);
        txtNome = findViewById(R.id.txtNome);
        txtSubtitulo = findViewById(R.id.txtSubtitulo);
        txtGasto = findViewById(R.id.txtGasto);
        txtExtra = findViewById(R.id.txtExtra);
        txtSemDespesas = findViewById(R.id.txtSemDespesas);
        txtSemAnotacoes = findViewById(R.id.txtSemAnotacoes);
        containerDespesas = findViewById(R.id.containerDespesas);
        containerAnotacoes = findViewById(R.id.containerAnotacoes);

        maquinaId = getIntent().getLongExtra(NavKeys.MAQUINA_ID, 0L);

        vm.obter(maquinaId).observe(this, m -> { if (m != null) { atual = m; mostrar(m); } });
        vm.totalDespesas(maquinaId).observe(this, t ->
                txtGasto.setText(MoneyUtils.formatar(t == null ? 0 : t)));
        vm.despesas(maquinaId).observe(this, this::renderDespesas);
        vm.anotacoes(maquinaId).observe(this, this::renderAnotacoes);

        findViewById(R.id.btnNovaAnotacao).setOnClickListener(v -> dialogAnotacao(null));
        findViewById(R.id.btnEditar).setOnClickListener(v -> {
            Intent i = new Intent(this, NovaMaquinaActivity.class);
            i.putExtra(NavKeys.MAQUINA_ID, maquinaId);
            startActivity(i);
        });
        findViewById(R.id.btnExcluir).setOnClickListener(v -> confirmarExclusao());
    }

    private void mostrar(Maquina m) {
        txtNome.setText(m.nome);
        StringBuilder sb = new StringBuilder();
        junta(sb, m.tipo); junta(sb, m.marca); junta(sb, m.modelo); junta(sb, m.ano);
        txtSubtitulo.setText(sb.toString());
        StringBuilder extra = new StringBuilder();
        if (m.identificacao != null && !m.identificacao.isEmpty())
            extra.append(getString(R.string.hint_maq_identificacao)).append(": ").append(m.identificacao);
        if (m.valor != null) {
            if (extra.length() > 0) extra.append("\n");
            extra.append(getString(R.string.hint_maq_valor)).append(": ").append(MoneyUtils.formatar(m.valor));
        }
        if (m.observacoes != null && !m.observacoes.isEmpty()) {
            if (extra.length() > 0) extra.append("\n");
            extra.append(m.observacoes);
        }
        txtExtra.setText(extra.toString());
        txtExtra.setVisibility(extra.length() == 0 ? View.GONE : View.VISIBLE);
    }

    private void junta(StringBuilder sb, String v) {
        if (v == null || v.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append(" • ");
        sb.append(v.trim());
    }

    private void renderDespesas(List<Despesa> lista) {
        containerDespesas.removeAllViews();
        boolean vazio = (lista == null || lista.isEmpty());
        txtSemDespesas.setVisibility(vazio ? View.VISIBLE : View.GONE);
        if (vazio) return;
        LayoutInflater inf = LayoutInflater.from(this);
        int cor = ContextCompat.getColor(this, R.color.despesa);
        for (Despesa d : lista) {
            View row = inf.inflate(R.layout.item_lancamento, containerDespesas, false);
            ((ImageView) row.findViewById(R.id.imgTipo)).setImageResource(R.drawable.ic_despesa);
            ((TextView) row.findViewById(R.id.txtDescricao)).setText(d.descricao);
            ((TextView) row.findViewById(R.id.txtData)).setText(DateUtils.isoParaBr(d.dataRegistro));
            TextView valor = row.findViewById(R.id.txtValor);
            valor.setText(MoneyUtils.comSinal(d.valor, false));
            valor.setTextColor(cor);
            row.setOnClickListener(v -> {
                Intent i = new Intent(this, DetalheLancamentoActivity.class);
                i.putExtra(NavKeys.LANCAMENTO_ID, d.id);
                i.putExtra(NavKeys.LANCAMENTO_TIPO, Lancamento.TIPO_DESPESA);
                startActivity(i);
            });
            containerDespesas.addView(row);
        }
    }

    private void renderAnotacoes(List<AnotacaoMaquina> lista) {
        containerAnotacoes.removeAllViews();
        boolean vazio = (lista == null || lista.isEmpty());
        txtSemAnotacoes.setVisibility(vazio ? View.VISIBLE : View.GONE);
        if (vazio) return;
        LayoutInflater inf = LayoutInflater.from(this);
        String hoje = DateUtils.hojeIso();
        for (AnotacaoMaquina a : lista) {
            View row = inf.inflate(R.layout.item_anotacao, containerAnotacoes, false);
            ((TextView) row.findViewById(R.id.txtTexto)).setText(a.texto);
            TextView data = row.findViewById(R.id.txtData);
            if (a.data != null && !a.data.isEmpty()) {
                data.setText(DateUtils.isoParaBr(a.data));
                data.setVisibility(View.VISIBLE);
            } else {
                data.setVisibility(View.GONE);
            }

            // Status: verde (realizada) / vermelho (atrasada) / amarelo (pendente no prazo) / neutro (sem prazo).
            // "Atrasada" é calculada: pendente com prazo vencido. Sem prazo, vale a data (regra antiga).
            String limite = prazoEfetivo(a);
            int corRes; int labelRes;
            if (a.concluida == 1) { corRes = R.color.receita; labelRes = R.string.tarefa_status_realizada; }
            else if (limite == null) { corRes = R.color.text_hint; labelRes = R.string.tarefa_status_pendente; }
            else if (limite.compareTo(hoje) < 0) { corRes = R.color.despesa; labelRes = R.string.tarefa_status_atrasada; }
            else { corRes = R.color.amber_500; labelRes = R.string.tarefa_status_pendente; }
            int cor = ContextCompat.getColor(this, corRes);
            row.findViewById(R.id.dotStatus).setBackgroundTintList(ColorStateList.valueOf(cor));
            TextView status = row.findViewById(R.id.txtStatus);
            status.setText(labelRes);
            status.setTextColor(cor);

            TextView info = row.findViewById(R.id.txtInfo);
            String textoInfo = infoTarefa(a);
            info.setText(textoInfo);
            info.setVisibility(textoInfo.isEmpty() ? View.GONE : View.VISIBLE);

            row.setOnClickListener(v -> menuAnotacao(a));
            containerAnotacoes.addView(row);
        }
    }

    /** Prazo da tarefa; anotações antigas (sem prazo) usam a data como antes. */
    private static String prazoEfetivo(AnotacaoMaquina a) {
        if (a.prazo != null && !a.prazo.isEmpty()) return a.prazo;
        return (a.data != null && !a.data.isEmpty()) ? a.data : null;
    }

    /** "Prazo: 10/10/2026 • Responsável: João" + "Feita por Maria em 05/10/2026" (o que existir). */
    private String infoTarefa(AnotacaoMaquina a) {
        StringBuilder sb = new StringBuilder();
        if (a.prazo != null && !a.prazo.isEmpty()) sb.append(getString(R.string.tarefa_prazo_x, DateUtils.isoParaBr(a.prazo)));
        if (a.responsavelNome != null && !a.responsavelNome.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(getString(R.string.tarefa_responsavel_x, a.responsavelNome));
        }
        // Quem criou/reportou e quando (data e hora; criado_em vem do servidor com hora desde 08/10).
        String quando = DateUtils.diaHoraDeLocal(a.criadoEm);
        boolean temQuem = a.criadoPorNome != null && !a.criadoPorNome.isEmpty();
        if (temQuem || !quando.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            if (temQuem && !quando.isEmpty()) sb.append(getString(R.string.tarefa_criada_por_em, a.criadoPorNome, quando));
            else if (temQuem) sb.append(getString(R.string.tarefa_criada_por, a.criadoPorNome));
            else sb.append(getString(R.string.tarefa_criada_em, quando));
        }
        if (a.concluida == 1 && a.concluidoPorNome != null && !a.concluidoPorNome.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(getString(R.string.tarefa_concluida_por, a.concluidoPorNome));
            String feitaEm = DateUtils.diaHoraDeInstante(a.concluidoEm);
            if (!feitaEm.isEmpty()) sb.append(" em ").append(feitaEm);
        }
        return sb.toString();
    }

    private void menuAnotacao(AnotacaoMaquina a) {
        boolean concluida = a.concluida == 1;
        String[] opcoes = {
                getString(concluida ? R.string.anotacao_marcar_pendente : R.string.anotacao_marcar_realizada),
                getString(R.string.acao_editar),
                getString(R.string.acao_excluir)
        };
        new AlertDialog.Builder(this)
                .setTitle(a.texto)
                .setItems(opcoes, (d, which) -> {
                    if (which == 1) { dialogAnotacao(a); return; }
                    if (which == 0) {
                        vm.marcarAnotacao(a, !concluida);
                        Toast.makeText(this, R.string.msg_anotacao_atualizada, Toast.LENGTH_SHORT).show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.acao_excluir)
                                .setMessage(a.texto)
                                .setPositiveButton(R.string.acao_excluir, (dd, ww) -> vm.excluirAnotacao(a))
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    }
                })
                .show();
    }

    /**
     * Nova tarefa (existente == null) ou edição. Tela 37: texto, data, prazo e responsável
     * (lista "pessoas" da equipe; sem equipe, o campo some).
     */
    private void dialogAnotacao(AnotacaoMaquina existente) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_nova_anotacao, null);
        TextInputEditText edtTexto = view.findViewById(R.id.edtTexto);
        TextInputEditText edtData = view.findViewById(R.id.edtData);
        TextInputEditText edtPrazo = view.findViewById(R.id.edtPrazo);
        MaskUtils.aplicarData(edtData);
        MaskUtils.aplicarData(edtPrazo);

        // Responsável: "Sem responsável" + pessoas da equipe (dono + membros aceitos).
        List<EquipeMinhaResponseDto.Pessoa> pessoas = EquipeRepository.pessoas(this);
        boolean temEquipe = pessoas.size() > 1;
        com.google.android.material.textfield.MaterialAutoCompleteTextView drop = view.findViewById(R.id.dropResponsavel);
        view.findViewById(R.id.tilResponsavel).setVisibility(temEquipe ? View.VISIBLE : View.GONE);
        List<String> nomes = new ArrayList<>();
        nomes.add(getString(R.string.tarefa_sem_responsavel));
        for (EquipeMinhaResponseDto.Pessoa p : pessoas) nomes.add(p.getNome() != null ? p.getNome() : p.getUsuarioUuid());
        drop.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomes));
        final int[] escolhido = {0}; // 0 = sem responsável; i = pessoas.get(i - 1)
        drop.setOnItemClickListener((parent, v, pos, id) -> escolhido[0] = pos > pessoas.size() ? -1 : pos);

        if (existente != null) {
            edtTexto.setText(existente.texto);
            if (existente.data != null && !existente.data.isEmpty()) edtData.setText(DateUtils.isoParaBr(existente.data));
            if (existente.prazo != null && !existente.prazo.isEmpty()) edtPrazo.setText(DateUtils.isoParaBr(existente.prazo));
            for (int i = 0; i < pessoas.size(); i++) {
                if (pessoas.get(i).getUsuarioUuid().equals(existente.responsavelUuid)) escolhido[0] = i + 1;
            }
            if (escolhido[0] == 0 && existente.responsavelNome != null && existente.responsavelUuid != null) {
                // Responsável que não está mais na lista (ex.: saiu da equipe): mantém enquanto não trocar.
                nomes.add(existente.responsavelNome);
                escolhido[0] = -1;
                drop.setText(existente.responsavelNome, false);
            }
        }
        if (escolhido[0] >= 0) drop.setText(nomes.get(escolhido[0]), false);

        new AlertDialog.Builder(this)
                .setTitle(existente == null ? R.string.maq_nova_anotacao : R.string.acao_editar)
                .setView(view)
                .setPositiveButton(R.string.acao_salvar, (d, w) -> {
                    String texto = edtTexto.getText() == null ? "" : edtTexto.getText().toString().trim();
                    if (texto.isEmpty()) { Toast.makeText(this, R.string.err_anotacao_texto, Toast.LENGTH_LONG).show(); return; }
                    AnotacaoMaquina a = existente != null ? existente : new AnotacaoMaquina();
                    a.maquinaId = maquinaId;
                    a.texto = texto;
                    String dataBr = edtData.getText() == null ? "" : edtData.getText().toString().trim();
                    a.data = dataBr.isEmpty() ? null : DateUtils.brParaIso(dataBr);
                    String prazoBr = edtPrazo.getText() == null ? "" : edtPrazo.getText().toString().trim();
                    a.prazo = prazoBr.isEmpty() ? null : DateUtils.brParaIso(prazoBr);
                    if (escolhido[0] == 0) {
                        a.responsavelUuid = null;
                        a.responsavelNome = null;
                    } else if (escolhido[0] > 0) {
                        EquipeMinhaResponseDto.Pessoa p = pessoas.get(escolhido[0] - 1);
                        a.responsavelUuid = p.getUsuarioUuid();
                        a.responsavelNome = p.getNome();
                    } // -1: mantém o responsável antigo
                    vm.salvarAnotacao(a);
                    Toast.makeText(this, R.string.msg_anotacao_salva, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmarExclusao() {
        if (atual == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.acao_excluir)
                .setMessage(atual.nome)
                .setPositiveButton(R.string.acao_excluir, (d, w) -> {
                    vm.excluir(atual);
                    Toast.makeText(this, R.string.msg_maquina_excluida, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
