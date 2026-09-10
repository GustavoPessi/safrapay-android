package com.tcc.gestaorural.ui.financeiro;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Receita;
import com.tcc.gestaorural.data.local.model.Lancamento;
import com.tcc.gestaorural.data.repository.DespesaRepository;
import com.tcc.gestaorural.data.repository.ReceitaRepository;
import com.tcc.gestaorural.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FinanceiroViewModel extends AndroidViewModel {

    /** Estado consolidado do extrato para uma safra + filtro. */
    public static class Extrato {
        public List<Lancamento> itens = new ArrayList<>();
        public double receitas, despesas, saldo;
    }

    public static final int FILTRO_TUDO = 0;
    public static final int FILTRO_MES = 1;

    private final ReceitaRepository receitaRepo;
    private final DespesaRepository despesaRepo;
    private final MediatorLiveData<Extrato> extrato = new MediatorLiveData<>();

    private List<Receita> ultReceitas = new ArrayList<>();
    private List<Despesa> ultDespesas = new ArrayList<>();
    private int filtro = FILTRO_TUDO;
    private boolean iniciado = false;

    public FinanceiroViewModel(@NonNull Application app) {
        super(app);
        receitaRepo = new ReceitaRepository(app);
        despesaRepo = new DespesaRepository(app);
    }

    public LiveData<Extrato> getExtrato() { return extrato; }

    public void iniciar(long safraId) {
        if (iniciado) return;
        iniciado = true;
        extrato.addSource(receitaRepo.listarPorSafra(safraId), r -> {
            ultReceitas = (r != null) ? r : new ArrayList<>();
            recombinar();
        });
        extrato.addSource(despesaRepo.listarPorSafra(safraId), d -> {
            ultDespesas = (d != null) ? d : new ArrayList<>();
            recombinar();
        });
    }

    public void iniciarGlobal(long usuarioId) {
        if (iniciado) return;
        iniciado = true;
        extrato.addSource(receitaRepo.listarPorUsuario(usuarioId), r -> {
            ultReceitas = (r != null) ? r : new ArrayList<>();
            recombinar();
        });
        extrato.addSource(despesaRepo.listarPorUsuario(usuarioId), d -> {
            ultDespesas = (d != null) ? d : new ArrayList<>();
            recombinar();
        });
    }

    public void setFiltro(int f) { filtro = f; recombinar(); }

    private void recombinar() {
        List<Lancamento> itens = new ArrayList<>();
        String mes = DateUtils.anoMesAtual();
        double totR = 0, totD = 0;

        for (Receita r : ultReceitas) {
            if (filtro == FILTRO_MES && (r.dataRegistro == null || !r.dataRegistro.startsWith(mes))) continue;
            Lancamento l = new Lancamento();
            l.id = r.id; l.tipo = Lancamento.TIPO_RECEITA; l.descricao = r.descricao;
            l.valor = r.valor; l.data = r.dataRegistro; l.safraId = r.safraId;
            itens.add(l); totR += r.valor;
        }
        for (Despesa d : ultDespesas) {
            if (filtro == FILTRO_MES && (d.dataRegistro == null || !d.dataRegistro.startsWith(mes))) continue;
            Lancamento l = new Lancamento();
            l.id = d.id; l.tipo = Lancamento.TIPO_DESPESA; l.descricao = d.descricao;
            l.valor = d.valor; l.data = d.dataRegistro; l.safraId = d.safraId;
            itens.add(l); totD += d.valor;
        }
        Collections.sort(itens, (a, b) -> {
            String da = a.data == null ? "" : a.data;
            String db = b.data == null ? "" : b.data;
            return db.compareTo(da); // mais recentes primeiro
        });

        Extrato e = new Extrato();
        e.itens = itens; e.receitas = totR; e.despesas = totD; e.saldo = totR - totD;
        extrato.setValue(e);
    }
}
