package com.tcc.gestaorural.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.ReceitaDao;
import com.tcc.gestaorural.data.local.entity.Receita;
import com.tcc.gestaorural.util.DateUtils;

import com.tcc.gestaorural.data.local.model.MesTotal;
import java.util.List;
import java.util.UUID;

public class ReceitaRepository {
    public interface Callback { void onConcluido(long id); }

    private final ReceitaDao dao;

    public ReceitaRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).receitaDao();
    }

    public LiveData<List<Receita>> listarPorSafra(long safraId) { return dao.observarPorSafra(safraId); }
    public LiveData<Double> totalPorSafra(long safraId) { return dao.totalPorSafra(safraId); }
    public LiveData<Receita> obter(long id) { return dao.observarPorId(id); }
    public LiveData<List<Receita>> listarPorUsuario(long usuarioId) { return dao.observarPorUsuario(usuarioId); }
    public LiveData<Double> totalPorUsuario(long usuarioId) { return dao.totalPorUsuario(usuarioId); }
    public LiveData<List<MesTotal>> totaisMensais(long usuarioId, String ini, String fim) { return dao.totaisMensais(usuarioId, ini, fim); }
    public LiveData<Double> totalNoPeriodo(long usuarioId, String ini, String fim) { return dao.totalNoPeriodo(usuarioId, ini, fim); }

    public void salvar(final Receita r, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            long id;
            if (r.dataRegistro == null || r.dataRegistro.isEmpty()) r.dataRegistro = DateUtils.hojeIso();
            if (r.id == 0L) {
                r.uuid = UUID.randomUUID().toString();
                r.criadoEm = DateUtils.agora();
                r.atualizadoEm = r.criadoEm;
                r.sincronizado = 0;
                id = dao.inserir(r);
            } else {
                r.atualizadoEm = DateUtils.agora();
                r.sincronizado = 0;
                dao.atualizar(r);
                id = r.id;
            }
            if (cb != null) cb.onConcluido(id);
        });
    }

    public void excluir(final Receita r) { AppDatabase.IO.execute(() -> dao.deletar(r)); }
}
