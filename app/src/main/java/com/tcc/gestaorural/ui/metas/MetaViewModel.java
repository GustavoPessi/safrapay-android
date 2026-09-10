package com.tcc.gestaorural.ui.metas;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.local.entity.MetaFinanceira;
import com.tcc.gestaorural.data.local.model.MetaResumo;
import com.tcc.gestaorural.data.local.model.TotalPorId;
import com.tcc.gestaorural.data.repository.MetaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaViewModel extends AndroidViewModel {

    /** Meta + progresso (derivado do saldo da safra vinculada). */
    public static class MetaProgresso {
        public MetaResumo meta;
        public double valorAtual;
        public int percentual;
    }

    private final MetaRepository repo;
    private final MediatorLiveData<List<MetaProgresso>> lista = new MediatorLiveData<>();
    private final MutableLiveData<Long> salvoComId = new MutableLiveData<>();

    private List<MetaResumo> ultMetas = new ArrayList<>();
    private final Map<Long, Double> recSafra = new HashMap<>();
    private final Map<Long, Double> desSafra = new HashMap<>();
    private boolean iniciado = false;

    public MetaViewModel(@NonNull Application app) { super(app); repo = new MetaRepository(app); }

    public LiveData<List<MetaProgresso>> getLista() { return lista; }
    public LiveData<Long> getSalvoComId() { return salvoComId; }
    public LiveData<MetaFinanceira> obter(long id) { return repo.obter(id); }

    public LiveData<Double> receitaTotalSafra(long safraId) { return repo.receitaTotalSafra(safraId); }
    public LiveData<Double> despesaTotalSafra(long safraId) { return repo.despesaTotalSafra(safraId); }

    public void iniciar(long usuarioId) {
        if (iniciado) return;
        iniciado = true;
        lista.addSource(repo.listarResumo(usuarioId), m -> { ultMetas = (m != null) ? m : new ArrayList<>(); recombinar(); });
        lista.addSource(repo.receitasPorSafra(usuarioId), r -> { preencher(recSafra, r); recombinar(); });
        lista.addSource(repo.despesasPorSafra(usuarioId), d -> { preencher(desSafra, d); recombinar(); });
    }

    private void preencher(Map<Long, Double> mapa, List<TotalPorId> linhas) {
        mapa.clear();
        if (linhas != null) for (TotalPorId t : linhas) mapa.put(t.id, t.total);
    }

    private double v(Map<Long, Double> mapa, long id) { Double x = mapa.get(id); return x == null ? 0 : x; }

    private void recombinar() {
        List<MetaProgresso> out = new ArrayList<>();
        for (MetaResumo m : ultMetas) {
            double saldo = Math.max(0, v(recSafra, m.safraId) - v(desSafra, m.safraId));
            MetaProgresso mp = new MetaProgresso();
            mp.meta = m;
            mp.valorAtual = saldo;
            mp.percentual = (m.valorAlvo > 0) ? (int) Math.min(100, Math.round(saldo / m.valorAlvo * 100)) : 0;
            out.add(mp);
        }
        lista.setValue(out);
    }

    public void salvar(MetaFinanceira m) { repo.salvar(m, salvoComId::postValue); }
    public void excluir(MetaFinanceira m) { repo.excluir(m); }
}
