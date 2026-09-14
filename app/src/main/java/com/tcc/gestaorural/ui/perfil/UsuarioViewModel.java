package com.tcc.gestaorural.ui.perfil;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.local.entity.Usuario;
import com.tcc.gestaorural.data.repository.UsuarioRepository;

public class UsuarioViewModel extends AndroidViewModel {

    public static class Evento {
        public final boolean ok; public final int erroResId;
        Evento(boolean ok, int erro) { this.ok = ok; this.erroResId = erro; }
    }

    private final UsuarioRepository repo;
    private final MutableLiveData<Evento> resultado = new MutableLiveData<>();
    private final MutableLiveData<Boolean> excluido = new MutableLiveData<>();

    public UsuarioViewModel(@NonNull Application app) {
        super(app);
        repo = new UsuarioRepository(app);
    }

    public LiveData<Usuario> obter(long id) { return repo.obter(id); }
    public LiveData<Evento> getResultado() { return resultado; }
    public LiveData<Boolean> getExcluido() { return excluido; }

    private final UsuarioRepository.Resultado cb = new UsuarioRepository.Resultado() {
        @Override public void ok() { resultado.postValue(new Evento(true, 0)); }
        @Override public void erro(int res) { resultado.postValue(new Evento(false, res)); }
    };

    public void salvarPerfil(Usuario u) { repo.atualizarPerfil(u, cb); }
    public void alterarSenha(long id, String atual, String nova) { repo.alterarSenha(id, atual, nova, cb); }
    public void excluirConta(long id) { repo.excluirConta(id, () -> excluido.postValue(true)); }
}
