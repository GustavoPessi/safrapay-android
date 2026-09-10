package com.tcc.gestaorural.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.CulturaDao;
import com.tcc.gestaorural.data.local.entity.Cultura;
import com.tcc.gestaorural.util.DateUtils;

import java.util.List;
import java.util.UUID;

public class CulturaRepository {
    public interface Callback { void onConcluido(long id); }

    private final CulturaDao dao;

    public CulturaRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).culturaDao();
    }

    public LiveData<List<Cultura>> listarPorPropriedade(long propriedadeId) { return dao.observarPorPropriedade(propriedadeId); }
    public LiveData<List<Cultura>> listarPorUsuario(long usuarioId) { return dao.observarPorUsuario(usuarioId); }
    public LiveData<Cultura> obter(long id) { return dao.observarPorId(id); }

    public void salvar(final Cultura c, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            long id;
            if (c.id == 0L) {
                c.uuid = UUID.randomUUID().toString();
                c.criadoEm = DateUtils.agora();
                c.atualizadoEm = c.criadoEm;
                c.sincronizado = 0;
                id = dao.inserir(c);
            } else {
                c.atualizadoEm = DateUtils.agora();
                c.sincronizado = 0;
                dao.atualizar(c);
                id = c.id;
            }
            if (cb != null) cb.onConcluido(id);
        });
    }

    public void excluir(final Cultura c) { AppDatabase.IO.execute(() -> dao.deletar(c)); }

    /**
     * Encontra (por nome, na propriedade) ou cria uma cultura, e devolve o id — usado no
     * cadastro de safra quando o usuário digita uma cultura nova. Roda no pool IO.
     */
    public long acharOuCriarSync(long propriedadeId, String nome) {
        Cultura existente = dao.buscarPorNome(propriedadeId, nome.trim());
        if (existente != null) return existente.id;
        Cultura c = new Cultura();
        c.uuid = UUID.randomUUID().toString();
        c.propriedadeId = propriedadeId;
        c.nome = nome.trim();
        c.criadoEm = DateUtils.agora();
        c.atualizadoEm = c.criadoEm;
        c.sincronizado = 0;
        return dao.inserir(c);
    }
}
