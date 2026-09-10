package com.tcc.gestaorural.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.DespesaDao;
import com.tcc.gestaorural.data.local.dao.MetaFinanceiraDao;
import com.tcc.gestaorural.data.local.dao.ReceitaDao;
import com.tcc.gestaorural.data.local.entity.MetaFinanceira;
import com.tcc.gestaorural.data.local.model.MetaResumo;
import com.tcc.gestaorural.data.local.model.TotalPorId;
import com.tcc.gestaorural.util.DateUtils;

import java.util.List;
import java.util.UUID;

public class MetaRepository {
    public interface Callback { void onConcluido(long id); }

    private final MetaFinanceiraDao dao;
    private final ReceitaDao receitaDao;
    private final DespesaDao despesaDao;

    public MetaRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.metaFinanceiraDao();
        this.receitaDao = db.receitaDao();
        this.despesaDao = db.despesaDao();
    }

    public LiveData<List<MetaResumo>> listarResumo(long usuarioId) { return dao.observarResumoPorUsuario(usuarioId); }
    public LiveData<MetaFinanceira> obter(long id) { return dao.observarPorId(id); }
    public LiveData<Integer> contarPorUsuario(long usuarioId) { return dao.contarPorUsuario(usuarioId); }

    // Totais agrupados por safra (para a lista de metas)
    public LiveData<List<TotalPorId>> receitasPorSafra(long usuarioId) { return receitaDao.totaisPorSafra(usuarioId); }
    public LiveData<List<TotalPorId>> despesasPorSafra(long usuarioId) { return despesaDao.totaisPorSafra(usuarioId); }

    // Totais de uma safra específica (para o Acompanhamento)
    public LiveData<Double> receitaTotalSafra(long safraId) { return receitaDao.totalPorSafra(safraId); }
    public LiveData<Double> despesaTotalSafra(long safraId) { return despesaDao.totalPorSafra(safraId); }

    public void salvar(final MetaFinanceira m, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            long id;
            if (m.id == 0L) {
                m.uuid = UUID.randomUUID().toString();
                m.criadoEm = DateUtils.agora();
                m.atualizadoEm = m.criadoEm;
                m.sincronizado = 0;
                id = dao.inserir(m);
            } else {
                m.atualizadoEm = DateUtils.agora();
                m.sincronizado = 0;
                dao.atualizar(m);
                id = m.id;
            }
            if (cb != null) cb.onConcluido(id);
        });
    }

    public void excluir(final MetaFinanceira m) { AppDatabase.IO.execute(() -> dao.deletar(m)); }
}
