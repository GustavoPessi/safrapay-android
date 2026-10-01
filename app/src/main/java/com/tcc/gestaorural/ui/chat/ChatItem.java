package com.tcc.gestaorural.ui.chat;

import com.tcc.gestaorural.data.remote.dto.ChatMensagemResponseDto;
import com.tcc.gestaorural.data.repository.ChatConsultaExecutor;

import java.util.List;

/** Item do RecyclerView do chat: bolha do usuário/IA, card de proposta, card de resultado ou botões de sugestão. */
public class ChatItem {
    public static final int USUARIO = 0;
    public static final int ASSISTENTE = 1;
    public static final int PROPOSTA = 2;
    public static final int RESULTADO = 3;
    public static final int SUGESTOES = 4;

    public final int tipo;
    public String texto;                                          // USUARIO / ASSISTENTE
    public List<ChatMensagemResponseDto.Lancamento> lancamentos;  // PROPOSTA
    public boolean resolvido;                                     // proposta já confirmada/descartada
    public String rodape;                                         // texto pós-resolução (ex.: "✓ Lançado")
    public boolean duplicataAvisada;                              // já avisou de possível lançamento repetido
    public ChatConsultaExecutor.Resultado resultado;              // RESULTADO
    public List<String> sugestoes;                                // SUGESTOES

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
    public static ChatItem resultado(ChatConsultaExecutor.Resultado r) {
        ChatItem c = new ChatItem(RESULTADO); c.resultado = r; return c;
    }
    public static ChatItem sugestoes(List<String> sugestoes) {
        ChatItem c = new ChatItem(SUGESTOES); c.sugestoes = sugestoes; return c;
    }
}
