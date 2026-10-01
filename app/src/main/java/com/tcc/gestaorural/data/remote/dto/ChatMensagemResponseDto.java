package com.tcc.gestaorural.data.remote.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resposta do POST /api/chat/mensagem. O backend NÃO grava nada: devolve a
 * proposta; o app mostra o card de confirmação e, no "sim", grava no Room pelo
 * mesmo caminho de uma despesa/receita cadastrada pela tela (e sincroniza).
 */
public class ChatMensagemResponseDto {

    /** PROPOSTA = lançamentos completos (pedir confirmação); PRECISA_INFORMACAO = falta algo; SEM_LANCAMENTO = conversa sem lançamento. */
    public String status;
    /** Texto pra mostrar na bolha da IA. */
    public String resposta;
    public List<Lancamento> lancamentos = new ArrayList<>();
    /** Quantas mensagens o usuário ainda pode mandar hoje (limite diário de custo). */
    public Integer mensagensRestantesHoje;

    public static class Lancamento {
        /** "DESPESA" ou "RECEITA". */
        public String tipo;
        public Double valor;
        /** yyyy-MM-dd */
        public String data;
        public String descricao;
        /** Nome exato de uma categoria do contexto (só despesa). */
        public String categoria;
        /** uuid de uma safra do contexto. */
        public String safraUuid;
        /** uuid de uma máquina do contexto (opcional, só despesa). */
        public String maquinaUuid;
        /** Campos que faltam pra gravar. Vazio = completo. */
        public List<String> camposFaltando = new ArrayList<>();
    }
}
