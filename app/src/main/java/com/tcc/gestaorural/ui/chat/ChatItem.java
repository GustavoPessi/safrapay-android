package com.tcc.gestaorural.ui.chat;

import com.tcc.gestaorural.data.remote.dto.ChatMensagemResponseDto;

import java.util.List;

/** Item exibido no RecyclerView do chat: bolha do usuário, da IA, ou um card de proposta. */
public class ChatItem {
    public static final int USUARIO = 0;
    public static final int ASSISTENTE = 1;
    public static final int PROPOSTA = 2;

    public final int tipo;
    public String texto;                                   // USUARIO / ASSISTENTE
    public List<ChatMensagemResponseDto.Lancamento> lancamentos;  // PROPOSTA
    public boolean resolvido;                              // proposta já confirmada/descartada
    public String rodape;                                 // texto pós-resolução (ex.: "✓ Lançado")

    private ChatItem(int tipo) { this.tipo = tipo; }

    public static ChatItem usuario(String texto) {
        ChatItem c = new ChatItem(USUARIO); c.texto = texto; return c;
    }
    public static ChatItem assistente(String texto) {
        ChatItem c = new ChatItem(ASSISTENTE); c.texto = texto; return c;
    }
    public static ChatItem proposta(List<ChatMensagemResponseDto.Lancamento> lancamentos) {
        ChatItem c = new ChatItem(PROPOSTA); c.lancamentos = lancamentos; return c;
    }
}
