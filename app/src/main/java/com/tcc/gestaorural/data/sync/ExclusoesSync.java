package com.tcc.gestaorural.data.sync;

import android.content.Context;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.data.local.entity.ExclusaoPendente;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.SessionManager;

/**
 * Exclusões sincronizadas (08/10/2026). Antes, excluir apagava só no celular e o próximo
 * pull trazia o item de volta. Agora cada exclusão feita pela pessoa é anotada aqui e sobe
 * na sincronização; o servidor exclui (com a mesma cascata do Room) e avisa os outros
 * aparelhos, que apagam também (ver SyncManager.enviarExclusoes/receberExclusoes).
 */
public final class ExclusoesSync {

    public static final String PROPRIEDADE = "propriedade";
    public static final String TALHAO = "talhao";
    public static final String SAFRA = "safra";
    public static final String RECEITA = "receita";
    public static final String DESPESA = "despesa";
    public static final String META = "meta";
    public static final String MAQUINA = "maquina";
    public static final String ANOTACAO = "anotacao_maquina";

    private ExclusoesSync() {}

    /**
     * Anota a exclusão de um item. Chamar fora da main thread, logo ANTES de apagar a linha
     * no Room. Só para exclusões feitas pela pessoa — apagar dados por outro motivo (troca de
     * conta, aplicar exclusão vinda do servidor) não passa por aqui.
     */
    public static void registrar(Context context, String entidade, String uuid) {
        if (uuid == null || uuid.isEmpty()) return;
        long usuarioId = new SessionManager(context).getUsuarioId();
        if (usuarioId == -1L) return;
        ExclusaoPendente e = new ExclusaoPendente();
        e.entidade = entidade;
        e.uuid = uuid;
        e.usuarioId = usuarioId;
        e.excluidoEm = DateUtils.paraIsoUtc(DateUtils.agora());
        AppDatabase.getInstance(context).exclusaoPendenteDao().inserir(e);
    }
}
