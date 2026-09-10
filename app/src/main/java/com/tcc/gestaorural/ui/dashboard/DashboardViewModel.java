package com.tcc.gestaorural.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.tcc.gestaorural.data.repository.DespesaRepository;
import com.tcc.gestaorural.data.repository.ReceitaRepository;

public class DashboardViewModel extends AndroidViewModel {

    public static class Resumo {
        public double receitas, despesas, saldo;
    }

    private final ReceitaRepository receitaRepo;
    private final DespesaRepository despesaRepo;
    private final MediatorLiveData<Resumo> resumo = new MediatorLiveData<>();

    private double ultReceitas = 0, ultDespesas = 0;
    private boolean iniciado = false;

    public DashboardViewModel(@NonNull Application app) {
        super(app);
        receitaRepo = new ReceitaRepository(app);
        despesaRepo = new DespesaRepository(app);
    }

    public LiveData<Resumo> getResumo() { return resumo; }

    public void iniciar(long usuarioId) {
        if (iniciado) return;
        iniciado = true;
        resumo.addSource(receitaRepo.totalPorUsuario(usuarioId), t -> {
            ultReceitas = (t != null) ? t : 0;
            recombinar();
        });
        resumo.addSource(despesaRepo.totalPorUsuario(usuarioId), t -> {
            ultDespesas = (t != null) ? t : 0;
            recombinar();
        });
    }

    private void recombinar() {
        Resumo r = new Resumo();
        r.receitas = ultReceitas;
        r.despesas = ultDespesas;
        r.saldo = ultReceitas - ultDespesas;
        resumo.setValue(r);
    }
}
