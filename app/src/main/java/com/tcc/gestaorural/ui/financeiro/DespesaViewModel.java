package com.tcc.gestaorural.ui.financeiro;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.local.entity.CategoriaDespesa;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.model.SafraOpcao;
import com.tcc.gestaorural.data.repository.DespesaRepository;
import com.tcc.gestaorural.data.repository.SafraRepository;

import java.util.List;

public class DespesaViewModel extends AndroidViewModel {
    private final DespesaRepository repo;
    private final SafraRepository safraRepo;
    private final MutableLiveData<Long> salvoComId = new MutableLiveData<>();

    public DespesaViewModel(@NonNull Application app) {
        super(app);
        repo = new DespesaRepository(app);
        safraRepo = new SafraRepository(app);
    }

    public LiveData<Despesa> obter(long id) { return repo.obter(id); }
    public LiveData<List<CategoriaDespesa>> categorias() { return repo.listarCategorias(); }
    public LiveData<List<SafraOpcao>> opcoesSafra(long usuarioId) { return safraRepo.observarOpcoes(usuarioId); }
    public LiveData<Long> getSalvoComId() { return salvoComId; }

    public void salvar(Despesa d) { repo.salvar(d, salvoComId::postValue); }
    public void excluir(Despesa d) { repo.excluir(d); }
}
