package com.tcc.gestaorural.ui.maquina;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.local.entity.AnotacaoMaquina;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.data.local.model.MaquinaTotal;
import com.tcc.gestaorural.data.repository.MaquinaRepository;

import java.util.List;

public class MaquinaViewModel extends AndroidViewModel {
    private final MaquinaRepository repo;
    private final MutableLiveData<Long> salvoComId = new MutableLiveData<>();

    public MaquinaViewModel(@NonNull Application app) {
        super(app);
        repo = new MaquinaRepository(app);
    }

    public LiveData<List<Maquina>> listar(long usuarioId) { return repo.listar(usuarioId); }
    public LiveData<Maquina> obter(long id) { return repo.obter(id); }
    public LiveData<Long> getSalvoComId() { return salvoComId; }
    public void salvar(Maquina m) { repo.salvar(m, salvoComId::postValue); }
    public void excluir(Maquina m) { repo.excluir(m); }

    public LiveData<List<Despesa>> despesas(long maquinaId) { return repo.despesas(maquinaId); }
    public LiveData<Double> totalDespesas(long maquinaId) { return repo.totalDespesas(maquinaId); }

    public LiveData<List<AnotacaoMaquina>> anotacoes(long maquinaId) { return repo.anotacoes(maquinaId); }
    public void salvarAnotacao(AnotacaoMaquina a) { repo.salvarAnotacao(a, null); }
    public void excluirAnotacao(AnotacaoMaquina a) { repo.excluirAnotacao(a); }
    public void marcarAnotacao(AnotacaoMaquina a, boolean concluida) { repo.marcarAnotacao(a, concluida); }

    /** Avisos de manutenção (pendentes vencidos ou nos próximos 7 dias). */
    public LiveData<List<com.tcc.gestaorural.data.local.model.AvisoManutencao>> avisos(long usuarioId) {
        return repo.avisos(usuarioId, 7);
    }

    /** Máquina + gasto total (para a lista). */
    public static class MaquinaComGasto {
        public Maquina maquina;
        public double gasto;
    }

    public LiveData<java.util.List<MaquinaComGasto>> listarComGasto(long usuarioId) {
        MediatorLiveData<java.util.List<MaquinaComGasto>> saida = new MediatorLiveData<>();
        final java.util.List<Maquina>[] ultMaquinas = new java.util.List[]{ new java.util.ArrayList<>() };
        final java.util.Map<Long, Double>[] totais = new java.util.Map[]{ new java.util.HashMap<>() };
        Runnable recombinar = () -> {
            java.util.List<MaquinaComGasto> out = new java.util.ArrayList<>();
            for (Maquina m : ultMaquinas[0]) {
                MaquinaComGasto mg = new MaquinaComGasto();
                mg.maquina = m;
                Double v = totais[0].get(m.id);
                mg.gasto = v == null ? 0 : v;
                out.add(mg);
            }
            saida.setValue(out);
        };
        saida.addSource(repo.listar(usuarioId), lista -> {
            ultMaquinas[0] = lista != null ? lista : new java.util.ArrayList<>();
            recombinar.run();
        });
        saida.addSource(repo.totaisPorMaquina(usuarioId), linhas -> {
            java.util.Map<Long, Double> mapa = new java.util.HashMap<>();
            if (linhas != null) for (MaquinaTotal t : linhas) mapa.put(t.maquinaId, t.total);
            totais[0] = mapa;
            recombinar.run();
        });
        return saida;
    }
}