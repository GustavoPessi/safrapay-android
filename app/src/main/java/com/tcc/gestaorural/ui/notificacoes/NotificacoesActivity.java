package com.tcc.gestaorural.ui.notificacoes;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.model.AvisoManutencao;
import com.tcc.gestaorural.data.local.model.Lancamento;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.financeiro.FinanceiroViewModel;
import com.tcc.gestaorural.ui.maquina.DetalheMaquinaActivity;
import com.tcc.gestaorural.ui.maquina.MaquinaViewModel;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.List;

public class NotificacoesActivity extends BaseActivity {

    private LinearLayout container, containerAvisos;
    private TextView txtVazio, txtAvisosTitulo;
    private boolean temAvisos = false, temLancamentos = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificacoes);
        setTitle(R.string.notif_title);

        container = findViewById(R.id.container);
        containerAvisos = findViewById(R.id.containerAvisos);
        txtVazio = findViewById(R.id.txtVazio);
        txtAvisosTitulo = findViewById(R.id.txtAvisosTitulo);

        long usuarioId = new SessionManager(this).getUsuarioId();

        MaquinaViewModel maquinaVM = new ViewModelProvider(this).get(MaquinaViewModel.class);
        maquinaVM.avisos(usuarioId).observe(this, this::renderAvisos);

        FinanceiroViewModel vm = new ViewModelProvider(this).get(FinanceiroViewModel.class);
        vm.iniciarGlobal(usuarioId);
        vm.getExtrato().observe(this, e -> { if (e != null) render(e.itens); });
    }

    private void renderAvisos(List<AvisoManutencao> avisos) {
        containerAvisos.removeAllViews();
        temAvisos = (avisos != null && !avisos.isEmpty());
        txtAvisosTitulo.setVisibility(temAvisos ? View.VISIBLE : View.GONE);
        if (temAvisos) {
            LayoutInflater inf = LayoutInflater.from(this);
            String hoje = DateUtils.hojeIso();
            for (AvisoManutencao av : avisos) {
                View row = inf.inflate(R.layout.item_anotacao, containerAvisos, false);
                ((TextView) row.findViewById(R.id.txtTexto)).setText(av.texto);
                TextView data = row.findViewById(R.id.txtData);
                data.setText(av.maquinaNome + " • " + DateUtils.isoParaBr(av.data));
                boolean atrasado = av.data.compareTo(hoje) < 0;
                int cor = ContextCompat.getColor(this, atrasado ? R.color.despesa : R.color.amber_500);
                row.findViewById(R.id.dotStatus).setBackgroundTintList(ColorStateList.valueOf(cor));
                TextView status = row.findViewById(R.id.txtStatus);
                status.setText(atrasado ? getString(R.string.aviso_atrasado)
                        : getString(R.string.aviso_prazo, DateUtils.isoParaBr(av.data)));
                status.setTextColor(cor);
                row.setOnClickListener(v -> {
                    Intent i = new Intent(this, DetalheMaquinaActivity.class);
                    i.putExtra(NavKeys.MAQUINA_ID, av.maquinaId);
                    startActivity(i);
                });
                containerAvisos.addView(row);
            }
        }
        atualizarVazio();
    }

    private void render(List<Lancamento> itens) {
        container.removeAllViews();
        temLancamentos = (itens != null && !itens.isEmpty());
        if (temLancamentos) {
            LayoutInflater inflater = LayoutInflater.from(this);
            int limite = Math.min(itens.size(), 20);
            for (int i = 0; i < limite; i++) {
                Lancamento l = itens.get(i);
                View row = inflater.inflate(R.layout.item_lancamento, container, false);
                boolean receita = l.isReceita();
                ((ImageView) row.findViewById(R.id.imgTipo)).setImageResource(
                        receita ? R.drawable.ic_receita : R.drawable.ic_despesa);
                ((TextView) row.findViewById(R.id.txtDescricao)).setText(
                        getString(receita ? R.string.notif_receita : R.string.notif_despesa) + ": " + l.descricao);
                ((TextView) row.findViewById(R.id.txtData)).setText(DateUtils.isoParaBr(l.data));
                TextView valor = row.findViewById(R.id.txtValor);
                valor.setText(MoneyUtils.comSinal(l.valor, receita));
                valor.setTextColor(ContextCompat.getColor(this, receita ? R.color.receita : R.color.despesa));
                container.addView(row);
            }
        }
        atualizarVazio();
    }

    private void atualizarVazio() {
        txtVazio.setVisibility((!temAvisos && !temLancamentos) ? View.VISIBLE : View.GONE);
    }
}
