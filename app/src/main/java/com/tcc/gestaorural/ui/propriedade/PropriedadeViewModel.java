package com.tcc.gestaorural.ui.propriedade;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.local.entity.Propriedade;
import com.tcc.gestaorural.data.repository.PropriedadeRepository;

import java.util.List;

public class PropriedadeViewModel extends AndroidViewModel {

    private final PropriedadeRepository repo;
    private final MutableLiveData<Long> salvoComId = new MutableLiveData<>();

    public PropriedadeViewModel(@NonNull Application app) {
        super(app);
        repo = new PropriedadeRepository(app);
    }

    public LiveData<List<Propriedade>> listar(long usuarioId) { return repo.listar(usuarioId); }
    public LiveData<Propriedade> obter(long id) { return repo.obter(id); }
    public LiveData<Long> getSalvoComId() { return salvoComId; }

    public void salvar(Propriedade p) {
        if (p.id == 0L) repo.salvarNova(p, salvoComId::postValue);
        else repo.atualizar(p, salvoComId::postValue);
    }

    public void excluir(Propriedade p) { repo.excluir(p); }
}
