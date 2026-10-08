package com.tcc.gestaorural.data.repository;

import com.tcc.gestaorural.data.sync.ExclusoesSync;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.PropriedadeDao;
import com.tcc.gestaorural.data.local.entity.Propriedade;
import com.tcc.gestaorural.util.DateUtils;

import java.util.List;
import java.util.UUID;

public class PropriedadeRepository {

    public interface Callback { void onConcluido(long id); }

    private final PropriedadeDao dao;

    private final Context ctx;

    public PropriedadeRepository(Context context) {
        this.ctx = context.getApplicationContext();
        this.dao = AppDatabase.getInstance(context).propriedadeDao();
    }

    public LiveData<List<Propriedade>> listar(long usuarioId) {
        return dao.observarPorUsuario(usuarioId);
    }

    public LiveData<Propriedade> obter(long id) {
        return dao.observarPorId(id);
    }

    public void salvarNova(final Propriedade p, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            p.uuid = UUID.randomUUID().toString();
            p.criadoEm = DateUtils.agora();
            p.atualizadoEm = p.criadoEm;
            p.sincronizado = 0;
            long id = dao.inserir(p);
            if (cb != null) cb.onConcluido(id);
        });
    }

    public void atualizar(final Propriedade p, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            p.atualizadoEm = DateUtils.agora();
            p.sincronizado = 0;
            dao.atualizar(p);
            if (cb != null) cb.onConcluido(p.id);
        });
    }

    public void excluir(final Propriedade p) {
        AppDatabase.IO.execute(() -> {
            ExclusoesSync.registrar(ctx, ExclusoesSync.PROPRIEDADE, p.uuid);
            dao.deletar(p);
        });
    }
}
