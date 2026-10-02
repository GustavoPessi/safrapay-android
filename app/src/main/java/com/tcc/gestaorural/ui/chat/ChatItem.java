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
    public static final int COMPROVANTE = 5;
    public static final int AUDIO = 6;

    /** Estado da transcrição da mensagem de voz (AUDIO). */
    public static final int TRANSCRICAO_PENDENTE = 0;
    public static final int TRANSCRICAO_OK = 1;
    public static final int TRANSCRICAO_INDISPONIVEL = 2;

    private final int tipo;
    private String texto;                                          // USUARIO / ASSISTENTE
    private List<ChatMensagemResponseDto.Lancamento> lancamentos;  // PROPOSTA
    private boolean resolvido;                                     // proposta já confirmada/descartada
    private String rodape;                                         // texto pós-resolução (ex.: "✓ Lançado")
    private boolean duplicataAvisada;                              // já avisou de possível lançamento repetido
    private ChatConsultaExecutor.Resultado resultado;              // RESULTADO
    private List<String> sugestoes;                                // SUGESTOES
    private ChatConsultaExecutor.Comprovante comprovante;          // COMPROVANTE
    private String caminhoAudio;                                   // AUDIO (texto = transcrição)
    private int duracaoSegundos;                                   // AUDIO
    private int estadoTranscricao;                                 // AUDIO
    private byte[] fotoJpeg;                                       // PROPOSTA vinda de foto → vira comprovante

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
    public static ChatItem audio(String caminho, int duracaoSegundos) {
        ChatItem c = new ChatItem(AUDIO);
        c.caminhoAudio = caminho;
        c.duracaoSegundos = duracaoSegundos;
        c.estadoTranscricao = TRANSCRICAO_PENDENTE;
        return c;
    }
    public static ChatItem comprovante(ChatConsultaExecutor.Comprovante comp) {
        ChatItem c = new ChatItem(COMPROVANTE); c.comprovante = comp; return c;
    }
    public static ChatItem sugestoes(List<String> sugestoes) {
        ChatItem c = new ChatItem(SUGESTOES); c.sugestoes = sugestoes; return c;
    }

    public int getTipo() { return tipo; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public List<ChatMensagemResponseDto.Lancamento> getLancamentos() { return lancamentos; }
    public void setLancamentos(List<ChatMensagemResponseDto.Lancamento> lancamentos) { this.lancamentos = lancamentos; }
    public boolean isResolvido() { return resolvido; }
    public void setResolvido(boolean resolvido) { this.resolvido = resolvido; }
    public String getRodape() { return rodape; }
    public void setRodape(String rodape) { this.rodape = rodape; }
    public boolean isDuplicataAvisada() { return duplicataAvisada; }
    public void setDuplicataAvisada(boolean duplicataAvisada) { this.duplicataAvisada = duplicataAvisada; }
    public ChatConsultaExecutor.Resultado getResultado() { return resultado; }
    public void setResultado(ChatConsultaExecutor.Resultado resultado) { this.resultado = resultado; }
    public List<String> getSugestoes() { return sugestoes; }
    public void setSugestoes(List<String> sugestoes) { this.sugestoes = sugestoes; }
    public ChatConsultaExecutor.Comprovante getComprovante() { return comprovante; }
    public String getCaminhoAudio() { return caminhoAudio; }
    public int getDuracaoSegundos() { return duracaoSegundos; }
    public int getEstadoTranscricao() { return estadoTranscricao; }
    public void setEstadoTranscricao(int estadoTranscricao) { this.estadoTranscricao = estadoTranscricao; }
    public byte[] getFotoJpeg() { return fotoJpeg; }
    public void setFotoJpeg(byte[] fotoJpeg) { this.fotoJpeg = fotoJpeg; }
}
