package com.tcc.gestaorural.ui.financeiro;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.local.entity.Receita;
import com.tcc.gestaorural.data.local.model.SafraOpcao;
import com.tcc.gestaorural.data.repository.ReceitaRepository;
import com.tcc.gestaorural.data.repository.SafraRepository;

import java.util.List;

public class ReceitaViewModel extends AndroidViewModel {
    private final ReceitaRepository repo;
    private final SafraRepository safraRepo;
    private final MutableLiveData<Long> salvoComId = new MutableLiveData<>();

    public ReceitaViewModel(@NonNull Application app) {
        super(app);
        repo = new ReceitaRepository(app);
        safraRepo = new SafraRepository(app);
    }

    public LiveData<Receita> obter(long id) { return repo.obter(id); }
    public LiveData<List<SafraOpcao>> opcoesSafra(long usuarioId) { return safraRepo.observarOpcoes(usuarioId); }
    public LiveData<Long> getSalvoComId() { return salvoComId; }

    public void salvar(Receita r) { repo.salvar(r, salvoComId::postValue); }
    public void excluir(Receita r) { repo.excluir(r); }
}
