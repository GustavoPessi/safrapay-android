package com.tcc.gestaorural.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.dao.SafraDao;
import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.data.local.model.SafraOpcao;
import com.tcc.gestaorural.data.local.model.SafraResumo;
import com.tcc.gestaorural.util.DateUtils;

import java.util.List;
import java.util.UUID;

public class SafraRepository {

    public interface Callback { void onConcluido(long id); }

    private final SafraDao dao;

    public SafraRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).safraDao();
    }

    public LiveData<List<SafraResumo>> listar(long talhaoId) { return dao.observarResumoPorTalhao(talhaoId); }
    public LiveData<Safra> obter(long id) { return dao.observarPorId(id); }
    public LiveData<SafraResumo> obterResumo(long id) { return dao.observarResumoPorId(id); }

    public LiveData<List<SafraOpcao>> observarOpcoes(long usuarioId) { return dao.observarOpcoesPorUsuario(usuarioId); }

    /** Salva a safra (talhão + ano). A cultura vem do talhão, não da safra. */
    public void salvar(final Safra s, final Callback cb) {
        AppDatabase.IO.execute(() -> {
            long id;
            if (s.id == 0L) {
                s.uuid = UUID.randomUUID().toString();
                s.criadoEm = DateUtils.agora();
                s.atualizadoEm = s.criadoEm;
                s.sincronizado = 0;
                id = dao.inserir(s);
            } else {
                s.atualizadoEm = DateUtils.agora();
                s.sincronizado = 0;
                dao.atualizar(s);
                id = s.id;
            }
            if (cb != null) cb.onConcluido(id);
        });
    }

    public void excluir(final Safra s) { AppDatabase.IO.execute(() -> dao.deletar(s)); }
}
