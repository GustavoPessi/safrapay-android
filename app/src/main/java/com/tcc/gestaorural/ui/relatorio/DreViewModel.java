package com.tcc.gestaorural.ui.relatorio;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.tcc.gestaorural.data.local.model.CategoriaTotal;
import com.tcc.gestaorural.data.repository.DespesaRepository;
import com.tcc.gestaorural.data.repository.ReceitaRepository;

import java.util.ArrayList;
import java.util.List;

/** DRE anual (Demonstração de Resultado do Exercício). */
public class DreViewModel extends AndroidViewModel {

    public static class Dre {
        public double receitas, impostos, receitaLiquida, custos, resultadoBruto, outras, resultado;
    }

    private final ReceitaRepository receitaRepo;
    private final DespesaRepository despesaRepo;
    private final MediatorLiveData<Dre> dre = new MediatorLiveData<>();

    private LiveData<Double> fonteReceita;
    private LiveData<List<CategoriaTotal>> fonteDespesa;
    private double ultReceitas = 0;
    private List<CategoriaTotal> ultCategorias = new ArrayList<>();

    public DreViewModel(@NonNull Application app) {
        super(app);
        receitaRepo = new ReceitaRepository(app);
        despesaRepo = new DespesaRepository(app);
    }

    public LiveData<Dre> getDre() { return dre; }

    public void definirAno(long usuarioId, int ano) {
        String ini = ano + "-01-01";
        String fim = ano + "-12-31";
        if (fonteReceita != null) dre.removeSource(fonteReceita);
        if (fonteDespesa != null) dre.removeSource(fonteDespesa);
        fonteReceita = receitaRepo.totalNoPeriodo(usuarioId, ini, fim);
        fonteDespesa = despesaRepo.totaisPorCategoria(usuarioId, ini, fim);
        dre.addSource(fonteReceita, r -> { ultReceitas = (r == null) ? 0 : r; recombinar(); });
        dre.addSource(fonteDespesa, c -> { ultCategorias = (c != null) ? c : new ArrayList<>(); recombinar(); });
    }

    private double catTotal(String nome) {
        for (CategoriaTotal c : ultCategorias) if (nome.equals(c.nome)) return c.total;
        return 0;
    }

    private void recombinar() {
        double totalDespesas = 0;
        for (CategoriaTotal c : ultCategorias) totalDespesas += c.total;
        Dre d = new Dre();
        d.receitas = ultReceitas;
        d.impostos = catTotal("Impostos");
        d.custos = catTotal("Sementes") + catTotal("Insumos") + catTotal("Combustível") + catTotal("Máquinas");
        d.outras = totalDespesas - d.impostos - d.custos;   // Documentos + Outras despesas + demais
        d.receitaLiquida = d.receitas - d.impostos;
        d.resultadoBruto = d.receitaLiquida - d.custos;
        d.resultado = d.resultadoBruto - d.outras;          // = receitas − total de despesas
        dre.setValue(d);
    }
}
