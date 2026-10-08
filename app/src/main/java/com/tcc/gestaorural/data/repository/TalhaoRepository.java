package com.tcc.gestaorural.data.repository;

import com.tcc.gestaorural.data.sync.ExclusoesSync;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.TalhaoDao;
import com.tcc.gestaorural.data.local.entity.Talhao;
import com.tcc.gestaorural.util.DateUtils;

import java.util.List;
import java.util.UUID;

public class TalhaoRepository {

    public interface Callback { void onConcluido(long id); }

    private final TalhaoDao dao;

    private final Context ctx;

    public TalhaoRepository(Context context) {
        this.ctx = context.getApplicationContext();
        this.dao = AppDatabase.getInstance(context).talhaoDao();
    }

    public LiveData<List<Talhao>> listar(long propriedadeId) { return dao.observarPorPropriedade(propriedadeId); }
    public LiveData<Talhao> obter(long id) { return dao.observarPorId(id); }
    public LiveData<Double> totalAreaPorUsuario(long usuarioId) { return dao.totalAreaPorUsuario(usuarioId); }

    public void salvar(final Talhao t, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            long id;
            if (t.id == 0L) {
                t.uuid = UUID.randomUUID().toString();
                t.criadoEm = DateUtils.agora();
                t.atualizadoEm = t.criadoEm;
                t.sincronizado = 0;
                id = dao.inserir(t);
            } else {
                t.atualizadoEm = DateUtils.agora();
                t.sincronizado = 0;
                dao.atualizar(t);
                id = t.id;
            }
            if (cb != null) cb.onConcluido(id);
        });
    }

    public void excluir(final Talhao t) {
        AppDatabase.IO.execute(() -> {
            ExclusoesSync.registrar(ctx, ExclusoesSync.TALHAO, t.uuid);
            dao.deletar(t);
        });
    }
}
