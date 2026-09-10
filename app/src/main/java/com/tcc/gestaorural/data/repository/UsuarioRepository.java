package com.tcc.gestaorural.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.UsuarioDao;
import com.tcc.gestaorural.data.local.entity.Usuario;
import com.tcc.gestaorural.data.remote.ApiService;
import com.tcc.gestaorural.data.remote.RetrofitClient;
import com.tcc.gestaorural.data.remote.dto.AlterarSenhaRequestDto;
import com.tcc.gestaorural.data.remote.dto.MensagemResponseDto;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.SecurityUtils;
import com.tcc.gestaorural.util.SessionManager;

import retrofit2.Call;
import retrofit2.Response;

public class UsuarioRepository {

    public interface Resultado { void ok(); void erro(int stringResId); }

    private final UsuarioDao dao;
    private final ApiService api;
    private final Context appContext;

    public UsuarioRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.dao = AppDatabase.getInstance(context).usuarioDao();
        this.api = RetrofitClient.getApi();
    }

    public LiveData<Usuario> obter(long id) { return dao.observarPorId(id); }

    public void atualizarPerfil(final Usuario u, final String emailNovo, final Resultado cb) {
        AppDatabase.IO.execute(() -> {
            String email = emailNovo.trim().toLowerCase();
            Usuario existentePorEmail = dao.buscarPorEmail(email);
            if (existentePorEmail != null && existentePorEmail.id != u.id) {
                cb.erro(com.tcc.gestaorural.R.string.err_email_existente);
                return;
            }
            if (u.cpf != null) {
                Usuario existentePorCpf = dao.buscarPorCpf(u.cpf);
                if (existentePorCpf != null && existentePorCpf.id != u.id) {
                    cb.erro(com.tcc.gestaorural.R.string.err_cpf_existente);
                    return;
                }
            }
            u.email = email;
            u.atualizadoEm = DateUtils.agora();
            u.sincronizado = 0;
            dao.atualizar(u);
            cb.ok();
        });
    }

    /**
     * Troca de senha do usuario logado. Agora fala com o backend (que exige a
     * senha atual pra confirmar), pois a senha de verdade mora la desde que
     * login/cadastro passaram a usar a API. So depois de o backend confirmar
     * a troca e que atualizamos o hash local, so pra manter as duas copias
     * coerentes (o hash local nao e mais usado pra autenticar).
     */
    public void alterarSenha(final long id, final String senhaAtual, final String novaSenha, final Resultado cb) {
        String token = new SessionManager(appContext).getToken();
        if (token == null) {
            cb.erro(com.tcc.gestaorural.R.string.err_servidor);
            return;
        }

        AlterarSenhaRequestDto request = new AlterarSenhaRequestDto(senhaAtual, novaSenha);
        api.alterarSenha("Bearer " + token, request).enqueue(new retrofit2.Callback<MensagemResponseDto>() {
            @Override
            public void onResponse(Call<MensagemResponseDto> call, Response<MensagemResponseDto> response) {
                if (response.isSuccessful()) {
                    AppDatabase.IO.execute(() -> {
                        dao.atualizarSenha(id, SecurityUtils.hashSenha(novaSenha), DateUtils.agora());
                        cb.ok();
                    });
                } else if (response.code() == 400) {
                    cb.erro(com.tcc.gestaorural.R.string.err_senha_atual_incorreta);
                } else if (response.code() == 401 || response.code() == 403) {
                    cb.erro(com.tcc.gestaorural.R.string.err_senha_atual_incorreta);
                } else {
                    cb.erro(com.tcc.gestaorural.R.string.err_servidor);
                }
            }

            @Override
            public void onFailure(Call<MensagemResponseDto> call, Throwable t) {
                cb.erro(com.tcc.gestaorural.R.string.err_servidor);
            }
        });
    }

    public void excluirConta(final long id, final Runnable done) {
        AppDatabase.IO.execute(() -> {
            dao.deletarPorId(id);
            if (done != null) done.run();
        });
    }
}
