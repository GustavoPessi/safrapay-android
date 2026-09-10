package com.tcc.gestaorural.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.UsuarioDao;
import com.tcc.gestaorural.data.local.entity.Usuario;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.SecurityUtils;

public class UsuarioRepository {

    public interface Resultado { void ok(); void erro(int stringResId); }

    private final UsuarioDao dao;

    public UsuarioRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).usuarioDao();
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

    public void alterarSenha(final long id, final String novaSenha, final Resultado cb) {
        AppDatabase.IO.execute(() -> {
            dao.atualizarSenha(id, SecurityUtils.hashSenha(novaSenha), DateUtils.agora());
            cb.ok();
        });
    }

    public void excluirConta(final long id, final Runnable done) {
        AppDatabase.IO.execute(() -> {
            dao.deletarPorId(id);
            if (done != null) done.run();
        });
    }
}
