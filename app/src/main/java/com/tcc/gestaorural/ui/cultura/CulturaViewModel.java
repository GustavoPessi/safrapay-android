package com.tcc.gestaorural.ui.cultura;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.local.entity.Cultura;
import com.tcc.gestaorural.data.repository.CulturaRepository;

import java.util.List;

public class CulturaViewModel extends AndroidViewModel {
    private final CulturaRepository repo;
    private final MutableLiveData<Long> salvoComId = new MutableLiveData<>();

    public CulturaViewModel(@NonNull Application app) { super(app); repo = new CulturaRepository(app); }

    public LiveData<List<Cultura>> listarPorPropriedade(long propriedadeId) { return repo.listarPorPropriedade(propriedadeId); }
    public LiveData<Cultura> obter(long id) { return repo.obter(id); }
    public LiveData<Long> getSalvoComId() { return salvoComId; }
    public void salvar(Cultura c) { repo.salvar(c, salvoComId::postValue); }
    public void excluir(Cultura c) { repo.excluir(c); }
}
