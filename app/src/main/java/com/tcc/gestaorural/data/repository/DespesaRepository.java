package com.tcc.gestaorural.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.CategoriaDespesaDao;
import com.tcc.gestaorural.data.local.dao.DespesaDao;
import com.tcc.gestaorural.data.local.entity.CategoriaDespesa;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.util.DateUtils;

import com.tcc.gestaorural.data.local.model.MesTotal;
import java.util.List;
import java.util.UUID;

public class DespesaRepository {
    public interface Callback { void onConcluido(long id); }

    private final DespesaDao dao;
    private final CategoriaDespesaDao categoriaDao;

    public DespesaRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.despesaDao();
        this.categoriaDao = db.categoriaDespesaDao();
    }

    public LiveData<List<Despesa>> listarPorSafra(long safraId) { return dao.observarPorSafra(safraId); }
    public LiveData<Double> totalPorSafra(long safraId) { return dao.totalPorSafra(safraId); }
    public LiveData<Despesa> obter(long id) { return dao.observarPorId(id); }
    public LiveData<List<Despesa>> listarPorUsuario(long usuarioId) { return dao.observarPorUsuario(usuarioId); }
    public LiveData<Double> totalPorUsuario(long usuarioId) { return dao.totalPorUsuario(usuarioId); }
    public LiveData<List<MesTotal>> totaisMensais(long usuarioId, String ini, String fim) { return dao.totaisMensais(usuarioId, ini, fim); }
    public LiveData<List<com.tcc.gestaorural.data.local.model.CategoriaTotal>> totaisPorCategoria(long usuarioId, String ini, String fim) { return dao.totaisPorCategoria(usuarioId, ini, fim); }
    public LiveData<List<CategoriaDespesa>> listarCategorias() { return categoriaDao.observarTodas(); }

    public void salvar(final Despesa d, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            long id;
            if (d.dataRegistro == null || d.dataRegistro.isEmpty()) d.dataRegistro = DateUtils.hojeIso();
            if (d.id == 0L) {
                d.uuid = UUID.randomUUID().toString();
                d.criadoEm = DateUtils.agora();
                d.atualizadoEm = d.criadoEm;
                d.sincronizado = 0;
                id = dao.inserir(d);
            } else {
                d.atualizadoEm = DateUtils.agora();
                d.sincronizado = 0;
                dao.atualizar(d);
                id = d.id;
            }
            if (cb != null) cb.onConcluido(id);
        });
    }

    public void excluir(final Despesa d) { AppDatabase.IO.execute(() -> dao.deletar(d)); }
}
