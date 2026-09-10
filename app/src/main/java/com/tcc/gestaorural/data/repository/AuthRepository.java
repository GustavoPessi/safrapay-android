package com.tcc.gestaorural.data.repository;

import android.content.Context;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.UsuarioDao;
import com.tcc.gestaorural.data.local.entity.Usuario;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.SecurityUtils;

import java.util.UUID;

/** Regras de cadastro/login. Roda tudo fora da main thread via AppDatabase.IO. */
public class AuthRepository {

    public interface Callback {
        void onSucesso(long usuarioId);
        void onErro(int stringResId);
    }

    private final UsuarioDao usuarioDao;

    public AuthRepository(Context context) {
        this.usuarioDao = AppDatabase.getInstance(context).usuarioDao();
    }

    public void cadastrar(final String nome, final String email, final String senha,
                          final String cpf, final String telefone, final String nascimento,
                          final Callback cb) {
        AppDatabase.IO.execute(() -> {
            String emailNorm = email.trim().toLowerCase();
            if (usuarioDao.contarPorEmail(emailNorm) > 0) {
                cb.onErro(com.tcc.gestaorural.R.string.err_email_existente);
                return;
            }
            // CPF guardado só com dígitos; bloqueia duplicidade entre contas.
            String cpfDigitos = MaskUtils.digitos(cpf);
            if (cpfDigitos.isEmpty()) cpfDigitos = null;
            if (cpfDigitos != null && usuarioDao.contarPorCpf(cpfDigitos) > 0) {
                cb.onErro(com.tcc.gestaorural.R.string.err_cpf_existente);
                return;
            }
            Usuario u = new Usuario();
            u.uuid = UUID.randomUUID().toString();
            u.nome = nome.trim();
            u.email = emailNorm;
            u.senhaHash = SecurityUtils.hashSenha(senha);
            u.cpf = cpfDigitos;
            u.telefone = vazioParaNull(telefone);
            u.nascimento = vazioParaNull(nascimento);
            u.criadoEm = DateUtils.agora();
            u.atualizadoEm = u.criadoEm;
            u.sincronizado = 0;
            long id = usuarioDao.inserir(u);
            cb.onSucesso(id);
        });
    }

    public void login(final String email, final String senha, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            Usuario u = usuarioDao.buscarPorEmail(email.trim().toLowerCase());
            if (u == null || !SecurityUtils.conferir(senha, u.senhaHash)) {
                cb.onErro(com.tcc.gestaorural.R.string.err_credenciais);
                return;
            }
            cb.onSucesso(u.id);
        });
    }

    private static String vazioParaNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
}
