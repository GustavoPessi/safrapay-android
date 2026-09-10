package com.tcc.gestaorural.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.tcc.gestaorural.data.repository.AuthRepository;

/** ViewModel de autenticação. Expõe o resultado como LiveData (migra bem p/ Kotlin+Flow). */
public class AuthViewModel extends AndroidViewModel {

    /** Resultado de uma operação de auth. */
    public static class Resultado {
        public final boolean sucesso;
        /** true quando o cadastro foi aceito mas precisa confirmar o e-mail antes de logar. */
        public final boolean pendente;
        public final long usuarioId;
        public final int erroResId;
        Resultado(boolean s, boolean p, long id, int erro) {
            sucesso = s; pendente = p; usuarioId = id; erroResId = erro;
        }
    }

    private final AuthRepository repo;
    private final MutableLiveData<Resultado> resultado = new MutableLiveData<>();
    private final MutableLiveData<Boolean> carregando = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository(app);
    }

    public LiveData<Resultado> getResultado() { return resultado; }
    public LiveData<Boolean> getCarregando() { return carregando; }

    private final AuthRepository.Callback cb = new AuthRepository.Callback() {
        @Override public void onSucesso(long usuarioId) {
            carregando.postValue(false);
            resultado.postValue(new Resultado(true, false, usuarioId, 0));
        }
        @Override public void onCadastroPendente() {
            carregando.postValue(false);
            resultado.postValue(new Resultado(false, true, -1, 0));
        }
        @Override public void onErro(int stringResId) {
            carregando.postValue(false);
            resultado.postValue(new Resultado(false, false, -1, stringResId));
        }
    };

    public void login(String email, String senha) {
        carregando.setValue(true);
        repo.login(email, senha, cb);
    }

    public void cadastrar(String nome, String email, String senha,
                          String cpf, String telefone, String nascimento) {
        carregando.setValue(true);
        repo.cadastrar(nome, email, senha, cpf, telefone, nascimento, cb);
    }
}
