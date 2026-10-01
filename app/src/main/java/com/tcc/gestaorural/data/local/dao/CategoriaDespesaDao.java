package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.tcc.gestaorural.data.local.entity.CategoriaDespesa;
import java.util.List;

@Dao
public interface CategoriaDespesaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void inserirTodas(List<CategoriaDespesa> categorias);

    @Query("SELECT COUNT(*) FROM categoria_despesa")
    int contar();

    @Query("SELECT * FROM categoria_despesa ORDER BY id")
    LiveData<List<CategoriaDespesa>> observarTodas();

    // --- Consultas sincronas usadas pelo SyncManager (roda em background, fora do LiveData) ---

    // Categoria viaja pelo NOME na sincronizacao (ids nao coincidem entre celular e servidor).
    @Query("SELECT * FROM categoria_despesa WHERE id = :id LIMIT 1")
    CategoriaDespesa buscarPorId(long id);

    @Query("SELECT * FROM categoria_despesa WHERE nome = :nome LIMIT 1")
    CategoriaDespesa buscarPorNome(String nome);

    /** Nomes das categorias (contexto do assistente de IA). */
    @Query("SELECT nome FROM categoria_despesa ORDER BY id")
    List<String> listarNomes();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long inserir(CategoriaDespesa c);
}
