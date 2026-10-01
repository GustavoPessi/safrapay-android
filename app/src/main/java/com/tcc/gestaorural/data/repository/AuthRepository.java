package com.tcc.gestaorural.data.repository;

import android.content.Context;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.UsuarioDao;
import com.tcc.gestaorural.data.local.entity.Usuario;
import com.tcc.gestaorural.data.remote.ApiService;
import com.tcc.gestaorural.data.remote.RetrofitClient;
import com.tcc.gestaorural.data.remote.dto.AuthResponseDto;
import com.tcc.gestaorural.data.remote.dto.LoginRequestDto;
import com.tcc.gestaorural.data.remote.dto.MensagemResponseDto;
import com.tcc.gestaorural.data.remote.dto.RegisterRequestDto;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.SecurityUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Regras de cadastro/login.
 *
 * FASE DE TESTES (backend SafraPay, Spring Boot na AWS): cadastro e login agora
 * falam com a API real em vez do banco local, pra validar confirmacao de
 * e-mail e redefinicao de senha de verdade no celular. Mantemos uma copia
 * local minima do usuario (tabela "usuario") so pra nao quebrar as telas que
 * ja dependem de um usuarioId local (propriedades, safras, financeiro etc,
 * que continuam 100% offline por enquanto). Quando o escopo do backend for
 * validado com o orientador, revisar se isso deve virar definitivo.
 */
public class AuthRepository {

    public interface Callback {
        void onSucesso(long usuarioId);
        /** Cadastro aceito pelo backend, mas precisa confirmar o e-mail antes de logar. */
        void onCadastroPendente();
        void onErro(int stringResId);
    }

    private final UsuarioDao usuarioDao;
    private final ApiService api;
    private final Context appContext;

    public AuthRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.usuarioDao = AppDatabase.getInstance(context).usuarioDao();
        this.api = RetrofitClient.getApi();
    }

    public void cadastrar(final String nome, final String email, final String senha,
                          final String cpf, final String telefone, final String nascimento,
                          final Callback cb) {
        String cpfDigitos = MaskUtils.digitos(cpf);
        RegisterRequestDto request = new RegisterRequestDto(
                nome.trim(),
                email.trim().toLowerCase(),
                senha,
                cpfDigitos.isEmpty() ? null : cpfDigitos,
                telefone == null || telefone.trim().isEmpty() ? null : telefone.trim(),
                DateUtils.brParaIso(nascimento));

        api.registrar(request).enqueue(new retrofit2.Callback<MensagemResponseDto>() {
            @Override
            public void onResponse(Call<MensagemResponseDto> call, Response<MensagemResponseDto> response) {
                if (response.isSuccessful()) {
                    cb.onCadastroPendente();
                } else if (response.code() == 409) {
                    cb.onErro(com.tcc.gestaorural.R.string.err_email_existente);
                } else {
                    cb.onErro(com.tcc.gestaorural.R.string.err_servidor);
                }
            }

            @Override
            public void onFailure(Call<MensagemResponseDto> call, Throwable t) {
                cb.onErro(com.tcc.gestaorural.R.string.err_servidor);
            }
        });
    }

    /**
     * Botão "Sair": revoga o refresh token no servidor (POST /api/auth/logout, sempre 200;
     * se estiver offline, ignora) e apaga a sessão local. Os dados do Room continuam no aparelho.
     */
    public void sair() {
        SessionManager session = new SessionManager(appContext);
        String refresh = session.getRefreshToken();
        session.logout();
        if (refresh == null) return;
        api.logout(new com.tcc.gestaorural.data.remote.dto.RefreshRequestDto(refresh))
                .enqueue(new retrofit2.Callback<MensagemResponseDto>() {
                    @Override public void onResponse(Call<MensagemResponseDto> c, Response<MensagemResponseDto> r) {}
                    @Override public void onFailure(Call<MensagemResponseDto> c, Throwable t) {}
                });
    }

    public void login(final String email, final String senha, final Callback cb) {
        LoginRequestDto request = new LoginRequestDto(email.trim().toLowerCase(), senha);

        api.login(request).enqueue(new retrofit2.Callback<AuthResponseDto>() {
            @Override
            public void onResponse(Call<AuthResponseDto> call, Response<AuthResponseDto> response) {
                final AuthResponseDto body = response.body();
                if (response.isSuccessful() && body != null) {
                    // Guarda o token de acesso (24h) e o refresh token (90 dias) — este
                    // permite renovar a sessão sozinho, sem o produtor digitar a senha de novo.
                    new SessionManager(appContext).salvarTokens(body.token, body.refreshToken);
                    AppDatabase.IO.execute(() -> {
                        long usuarioId = garantirUsuarioLocal(body, senha);
                        cb.onSucesso(usuarioId);
                    });
                } else if (response.code() == 403) {
                    cb.onErro(com.tcc.gestaorural.R.string.err_email_nao_confirmado);
                } else if (response.code() == 401) {
                    cb.onErro(com.tcc.gestaorural.R.string.err_credenciais);
                } else {
                    cb.onErro(com.tcc.gestaorural.R.string.err_servidor);
                }
            }

            @Override
            public void onFailure(Call<AuthResponseDto> call, Throwable t) {
                cb.onErro(com.tcc.gestaorural.R.string.err_servidor);
            }
        });
    }

    /**
     * Garante que existe uma linha local pro usuario autenticado no backend,
     * so pra manter funcionando as telas offline que ainda usam usuarioId
     * local. A senha aqui NAO e mais a fonte de verdade (quem autentica e o
     * backend) - so preenchemos a coluna NOT NULL local.
     *
     * cpf/telefone/nascimento vem do backend (fonte de verdade hoje, ja que
     * "Editar perfil" ainda so grava local - ver observacao la). Numa conta
     * ja existente localmente, so preenchemos os campos que ainda estiverem
     * vazios aqui, pra nao sobrescrever nada que o usuario tenha digitado em
     * "Editar perfil" e que ainda nao tenha ido pro servidor.
     */
    private long garantirUsuarioLocal(AuthResponseDto body, String senha) {
        Usuario existente = usuarioDao.buscarPorEmail(body.email);
        if (existente != null) {
            boolean mudou = false;
            if (existente.cpf == null && body.cpf != null) { existente.cpf = body.cpf; mudou = true; }
            if (existente.telefone == null && body.telefone != null) { existente.telefone = body.telefone; mudou = true; }
            if (existente.nascimento == null && body.nascimento != null) {
                existente.nascimento = DateUtils.isoParaBr(body.nascimento);
                mudou = true;
            }
            if (mudou) {
                existente.atualizadoEm = DateUtils.agora();
                usuarioDao.atualizar(existente);
            }
            return existente.id;
        }

        Usuario u = new Usuario();
        u.uuid = UUID.randomUUID().toString();
        u.nome = body.nome;
        u.email = body.email;
        u.senhaHash = SecurityUtils.hashSenha(senha);
        u.cpf = body.cpf;
        u.telefone = body.telefone;
        u.nascimento = body.nascimento == null ? null : DateUtils.isoParaBr(body.nascimento);
        u.criadoEm = DateUtils.agora();
        u.atualizadoEm = u.criadoEm;
        u.sincronizado = 0;
        return usuarioDao.inserir(u);
    }
}
