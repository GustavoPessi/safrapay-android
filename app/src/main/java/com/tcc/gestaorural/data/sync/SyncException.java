package com.tcc.gestaorural.data.sync;

/** Erro de sincronizacao com uma mensagem já pronta pra mostrar ao usuário. */
public class SyncException extends RuntimeException {
    public SyncException(String mensagem) {
        super(mensagem);
    }
}
