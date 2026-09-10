package com.tcc.gestaorural.ui.safra;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.data.local.model.SafraResumo;
import com.tcc.gestaorural.data.repository.SafraRepository;

import java.util.List;

public class SafraViewModel extends AndroidViewModel {
    private final SafraRepository repo;
    private final MutableLiveData<Long> salvoComId = new MutableLiveData<>();

    public SafraViewModel(@NonNull Application app) { super(app); repo = new SafraRepository(app); }

    public LiveData<List<SafraResumo>> listar(long talhaoId) { return repo.listar(talhaoId); }
    public LiveData<Safra> obter(long id) { return repo.obter(id); }
    public LiveData<SafraResumo> obterResumo(long id) { return repo.obterResumo(id); }
    public LiveData<Long> getSalvoComId() { return salvoComId; }

    /** Salva a safra (talhão + ano). A cultura vem do talhão. */
    public void salvar(Safra s) { repo.salvar(s, salvoComId::postValue); }
    public void excluir(Safra s) { repo.excluir(s); }
}
