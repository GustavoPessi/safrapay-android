package com.tcc.gestaorural.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tcc.gestaorural.data.local.entity.Usuario;

@Dao
public interface UsuarioDao {
    @Insert long inserir(Usuario usuario);
    @Update void atualizar(Usuario usuario);
    @Delete void deletar(Usuario usuario);

    @Query("SELECT * FROM usuario WHERE email = :email LIMIT 1")
    Usuario buscarPorEmail(String email);

    @Query("SELECT COUNT(*) FROM usuario WHERE email = :email")
    int contarPorEmail(String email);

    @Query("SELECT COUNT(*) FROM usuario WHERE cpf = :cpf")
    int contarPorCpf(String cpf);

    @Query("SELECT * FROM usuario WHERE cpf = :cpf LIMIT 1")
    Usuario buscarPorCpf(String cpf);

    @Query("SELECT * FROM usuario WHERE id = :id LIMIT 1")
    LiveData<Usuario> observarPorId(long id);

    @Query("UPDATE usuario SET senha_hash = :hash, atualizado_em = :agora, sincronizado = 0 WHERE id = :id")
    void atualizarSenha(long id, String hash, String agora);

    @Query("DELETE FROM usuario WHERE id = :id")
    void deletarPorId(long id);

    /** Leitura síncrona (thread de IO): usado pra refazer o login depois de alterar a senha. */
    @Query("SELECT * FROM usuario WHERE id = :id LIMIT 1")
    Usuario buscarPorId(long id);
}
