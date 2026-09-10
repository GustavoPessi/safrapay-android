package com.tcc.gestaorural.ui.talhao;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.local.entity.Talhao;
import com.tcc.gestaorural.data.repository.TalhaoRepository;

import java.util.List;

public class TalhaoViewModel extends AndroidViewModel {
    private final TalhaoRepository repo;
    private final MutableLiveData<Long> salvoComId = new MutableLiveData<>();

    public TalhaoViewModel(@NonNull Application app) { super(app); repo = new TalhaoRepository(app); }

    public LiveData<List<Talhao>> listar(long propriedadeId) { return repo.listar(propriedadeId); }
    public LiveData<Talhao> obter(long id) { return repo.obter(id); }
    public LiveData<Long> getSalvoComId() { return salvoComId; }

    public void salvar(Talhao t) { repo.salvar(t, salvoComId::postValue); }
    public void excluir(Talhao t) { repo.excluir(t); }
}
