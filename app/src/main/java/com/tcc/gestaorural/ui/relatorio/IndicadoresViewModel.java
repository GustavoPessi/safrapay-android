package com.tcc.gestaorural.ui.relatorio;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.tcc.gestaorural.data.repository.DespesaRepository;
import com.tcc.gestaorural.data.repository.MetaRepository;
import com.tcc.gestaorural.data.repository.ReceitaRepository;
import com.tcc.gestaorural.data.repository.TalhaoRepository;

public class IndicadoresViewModel extends AndroidViewModel {

    public static class Indicadores {
        public double receitas, despesas, saldo, area;
        public double margem;        // %
        public double receitaPorHa, custoPorHa;
        public int metasAtivas;
    }

    private final ReceitaRepository receitaRepo;
    private final DespesaRepository despesaRepo;
    private final TalhaoRepository talhaoRepo;
    private final MetaRepository metaRepo;
    private final MediatorLiveData<Indicadores> dados = new MediatorLiveData<>();

    private double receitas = 0, despesas = 0, area = 0;
    private int metas = 0;
    private boolean iniciado = false;

    public IndicadoresViewModel(@NonNull Application app) {
        super(app);
        receitaRepo = new ReceitaRepository(app);
        despesaRepo = new DespesaRepository(app);
        talhaoRepo = new TalhaoRepository(app);
        metaRepo = new MetaRepository(app);
    }

    public LiveData<Indicadores> getDados() { return dados; }

    public void iniciar(long usuarioId) {
        if (iniciado) return;
        iniciado = true;
        dados.addSource(receitaRepo.totalPorUsuario(usuarioId), t -> { receitas = nz(t); recombinar(); });
        dados.addSource(despesaRepo.totalPorUsuario(usuarioId), t -> { despesas = nz(t); recombinar(); });
        dados.addSource(talhaoRepo.totalAreaPorUsuario(usuarioId), t -> { area = nz(t); recombinar(); });
        dados.addSource(metaRepo.contarPorUsuario(usuarioId), c -> { metas = (c != null) ? c : 0; recombinar(); });
    }

    private void recombinar() {
        Indicadores i = new Indicadores();
        i.receitas = receitas; i.despesas = despesas; i.saldo = receitas - despesas; i.area = area;
        i.margem = (receitas > 0) ? (i.saldo / receitas * 100) : 0;
        i.receitaPorHa = (area > 0) ? (receitas / area) : 0;
        i.custoPorHa = (area > 0) ? (despesas / area) : 0;
        i.metasAtivas = metas;
        dados.setValue(i);
    }

    private double nz(Double d) { return d == null ? 0 : d; }
}
