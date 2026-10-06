package com.tcc.gestaorural.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.AnotacaoMaquinaDao;
import com.tcc.gestaorural.data.local.dao.DespesaDao;
import com.tcc.gestaorural.data.local.dao.MaquinaDao;
import com.tcc.gestaorural.data.local.entity.AnotacaoMaquina;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.data.local.model.AvisoManutencao;
import com.tcc.gestaorural.data.local.model.MaquinaTotal;
import com.tcc.gestaorural.util.DateUtils;

import java.util.List;
import java.util.UUID;

public class MaquinaRepository {
    public interface Callback { void onConcluido(long id); }

    private final MaquinaDao dao;
    private final AnotacaoMaquinaDao anotacaoDao;
    private final DespesaDao despesaDao;

    public MaquinaRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.maquinaDao();
        this.anotacaoDao = db.anotacaoMaquinaDao();
        this.despesaDao = db.despesaDao();
    }

    // ---- Máquinas ----
    public LiveData<List<Maquina>> listar(long usuarioId) { return dao.observarPorUsuario(usuarioId); }
    public LiveData<Maquina> obter(long id) { return dao.observarPorId(id); }

    public void salvar(final Maquina m, final Callback cb) {
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

    /** Exclui a máquina; antes, desvincula as despesas (maquina_id = NULL). */
    public void excluir(final Maquina m) {
        AppDatabase.IO.execute(() -> {
            despesaDao.desvincularMaquina(m.id);
            dao.deletar(m);
        });
    }

    // ---- Despesas da máquina ----
    public LiveData<List<Despesa>> despesas(long maquinaId) { return despesaDao.observarPorMaquina(maquinaId); }
    public LiveData<Double> totalDespesas(long maquinaId) { return despesaDao.totalPorMaquina(maquinaId); }
    public LiveData<List<MaquinaTotal>> totaisPorMaquina(long usuarioId) { return despesaDao.totaisPorMaquina(usuarioId); }

    // ---- Anotações ----
    public LiveData<List<AnotacaoMaquina>> anotacoes(long maquinaId) { return anotacaoDao.observarPorMaquina(maquinaId); }

    /** Cria (id = 0) ou edita a anotação/tarefa; sobe no próximo sync. */
    public void salvarAnotacao(final AnotacaoMaquina a, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            long id;
            if (a.id == 0L) {
                a.uuid = UUID.randomUUID().toString();
                a.criadoEm = DateUtils.agora();
                a.atualizadoEm = a.criadoEm;
                a.sincronizado = 0;
                id = anotacaoDao.inserir(a);
            } else {
                a.atualizadoEm = DateUtils.agora();
                a.sincronizado = 0;
                anotacaoDao.atualizar(a);
                id = a.id;
            }
            if (cb != null) cb.onConcluido(id);
        });
    }

    public void excluirAnotacao(final AnotacaoMaquina a) {
        AppDatabase.IO.execute(() -> anotacaoDao.deletar(a));
    }

    /** Marca a anotação como concluída (1) ou pendente (0). */
    public void marcarAnotacao(final AnotacaoMaquina a, final boolean concluida) {
        AppDatabase.IO.execute(() -> {
            a.concluida = concluida ? 1 : 0;
            a.atualizadoEm = DateUtils.agora();
            a.sincronizado = 0;
            anotacaoDao.atualizar(a);
        });
    }

    /** Avisos de manutenção do usuário: pendentes com data vencida ou nos próximos {@code diasAntecedencia} dias. */
    public LiveData<List<AvisoManutencao>> avisos(long usuarioId, int diasAntecedencia) {
        return anotacaoDao.avisos(usuarioId, DateUtils.hojeMaisDias(diasAntecedencia));
    }
}
