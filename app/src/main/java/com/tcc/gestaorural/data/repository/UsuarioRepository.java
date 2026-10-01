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
import com.tcc.gestaorural.data.remote.dto.PerfilRequestDto;
import com.tcc.gestaorural.data.remote.dto.PerfilResponseDto;
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

    /**
     * Atualiza nome/CPF/telefone/nascimento no backend (fonte de verdade) e,
     * so depois de confirmado, espelha a copia local. O e-mail nao entra
     * aqui de proposito: mudar o e-mail de uma conta ja verificada exigiria
     * um fluxo de reconfirmacao que ainda nao existe - por isso o campo de
     * e-mail fica so-leitura na tela "Editar perfil".
     */
    public void atualizarPerfil(final Usuario u, final Resultado cb) {
        String token = new SessionManager(appContext).getToken();
        if (token == null) {
            cb.erro(com.tcc.gestaorural.R.string.err_servidor);
            return;
        }

        PerfilRequestDto request = new PerfilRequestDto();
        request.nome = u.nome;
        request.cpf = u.cpf;
        request.telefone = u.telefone;
        request.nascimento = DateUtils.brParaIso(u.nascimento);

        api.atualizarPerfil("Bearer " + token, request).enqueue(new retrofit2.Callback<PerfilResponseDto>() {
            @Override
            public void onResponse(Call<PerfilResponseDto> call, Response<PerfilResponseDto> response) {
                PerfilResponseDto body = response.body();
                if (response.isSuccessful() && body != null) {
                    AppDatabase.IO.execute(() -> {
                        u.nome = body.nome;
                        u.cpf = body.cpf;
                        u.telefone = body.telefone;
                        u.nascimento = body.nascimento == null ? null : DateUtils.isoParaBr(body.nascimento);
                        u.atualizadoEm = DateUtils.agora();
                        u.sincronizado = 1;
                        dao.atualizar(u);
                        cb.ok();
                    });
                } else if (response.code() == 400) {
                    cb.erro(com.tcc.gestaorural.R.string.err_cpf_invalido);
                } else {
                    cb.erro(com.tcc.gestaorural.R.string.err_servidor);
                }
            }

            @Override
            public void onFailure(Call<PerfilResponseDto> call, Throwable t) {
                cb.erro(com.tcc.gestaorural.R.string.err_servidor);
            }
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
                        // O servidor encerra todas as sessões ao trocar a senha: refaz o
                        // login com a senha nova pra pegar token + refresh token novos.
                        reautenticar(id, novaSenha);
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

    /** Roda na thread de IO. Se falhar (sem sinal), a próxima chamada cai no fluxo de sessão expirada. */
    private void reautenticar(long id, String novaSenha) {
        try {
            Usuario u = dao.buscarPorId(id);
            if (u == null || u.email == null) return;
            Response<com.tcc.gestaorural.data.remote.dto.AuthResponseDto> r = api.login(
                    new com.tcc.gestaorural.data.remote.dto.LoginRequestDto(u.email, novaSenha)).execute();
            if (r.isSuccessful() && r.body() != null && r.body().token != null) {
                new SessionManager(appContext).salvarTokens(r.body().token, r.body().refreshToken);
            }
        } catch (Exception ignored) {
            // sem rede: o produtor vai precisar entrar de novo depois
        }
    }

    public void excluirConta(final long id, final Runnable done) {
        AppDatabase.IO.execute(() -> {
            dao.deletarPorId(id);
            if (done != null) done.run();
        });
    }
}
