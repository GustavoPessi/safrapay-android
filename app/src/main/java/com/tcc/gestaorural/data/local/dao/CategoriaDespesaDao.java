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
}
