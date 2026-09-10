package com.tcc.gestaorural.ui.relatorio;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.tcc.gestaorural.data.local.model.MesTotal;
import com.tcc.gestaorural.data.repository.DespesaRepository;
import com.tcc.gestaorural.data.repository.ReceitaRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class RelatorioViewModel extends AndroidViewModel {

    public static class MesLinha {
        public String mes;      // yyyy-MM
        public double receita, despesa;
    }

    public static class Relatorio {
        public List<MesLinha> meses = new ArrayList<>();
        public double totalReceitas, totalDespesas, saldo;
    }

    private final ReceitaRepository receitaRepo;
    private final DespesaRepository despesaRepo;
    private final MediatorLiveData<Relatorio> relatorio = new MediatorLiveData<>();

    private LiveData<List<MesTotal>> fonteReceitas, fonteDespesas;
    private List<MesTotal> ultReceitas = new ArrayList<>();
    private List<MesTotal> ultDespesas = new ArrayList<>();

    private long usuarioId;

    public RelatorioViewModel(@NonNull Application app) {
        super(app);
        receitaRepo = new ReceitaRepository(app);
        despesaRepo = new DespesaRepository(app);
    }

    public LiveData<Relatorio> getRelatorio() { return relatorio; }

    public void definirPeriodo(long usuarioId, String ini, String fim) {
        this.usuarioId = usuarioId;
        if (fonteReceitas != null) relatorio.removeSource(fonteReceitas);
        if (fonteDespesas != null) relatorio.removeSource(fonteDespesas);
        fonteReceitas = receitaRepo.totaisMensais(usuarioId, ini, fim);
        fonteDespesas = despesaRepo.totaisMensais(usuarioId, ini, fim);
        relatorio.addSource(fonteReceitas, r -> { ultReceitas = (r != null) ? r : new ArrayList<>(); recombinar(); });
        relatorio.addSource(fonteDespesas, d -> { ultDespesas = (d != null) ? d : new ArrayList<>(); recombinar(); });
    }

    private void recombinar() {
        Map<String, Double> rec = new LinkedHashMap<>();
        Map<String, Double> des = new LinkedHashMap<>();
        TreeSet<String> meses = new TreeSet<>();
        for (MesTotal m : ultReceitas) { rec.put(m.mes, m.total); meses.add(m.mes); }
        for (MesTotal m : ultDespesas) { des.put(m.mes, m.total); meses.add(m.mes); }

        Relatorio r = new Relatorio();
        double totR = 0, totD = 0;
        for (String mes : meses) {
            MesLinha l = new MesLinha();
            l.mes = mes;
            l.receita = rec.containsKey(mes) ? rec.get(mes) : 0;
            l.despesa = des.containsKey(mes) ? des.get(mes) : 0;
            totR += l.receita; totD += l.despesa;
            r.meses.add(l);
        }
        r.totalReceitas = totR; r.totalDespesas = totD; r.saldo = totR - totD;
        relatorio.setValue(r);
    }
}
