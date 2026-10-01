package com.tcc.gestaorural.data.remote.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resposta do POST /api/chat/mensagem. O backend NÃO grava nada:
 * - PROPOSTA / PRECISA_INFORMACAO: devolve lançamentos; o app mostra o card de
 *   confirmação e, no "sim", grava no Room (mesmo caminho de um cadastro normal).
 * - CONSULTA (v2): devolve só a pergunta estruturada ("consulta"); o app
 *   calcula no Room e mostra o resultado. A IA nunca manda números.
 * - SEM_LANCAMENTO: só conversa.
 */
public class ChatMensagemResponseDto {

    /** PROPOSTA | PRECISA_INFORMACAO | SEM_LANCAMENTO | CONSULTA (v2) */
    public String status;
    /** Texto pra mostrar na bolha da IA (na CONSULTA, só uma introdução sem números). */
    public String resposta;
    public List<Lancamento> lancamentos = new ArrayList<>();
    /** Quantas mensagens o usuário ainda pode mandar hoje (limite diário de custo). */
    public Integer mensagensRestantesHoje;
    /** v2: só quando status = CONSULTA. */
    public Consulta consulta;
    /** v2: respostas rápidas (o app mostra como botões; tocar envia o texto). */
    public List<String> sugestoes;
    /** Avisos do servidor (ex.: arquivo grande cortado). */
    public List<String> avisos;

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

    /** v2: pergunta estruturada que o app executa no Room. */
    public static class Consulta {
        /** GASTOS | RECEITAS | SALDO | LANCAMENTOS | SAFRAS | AREA | METAS | MAQUINAS */
        public String tipo;
        /** yyyy-MM-dd ou null */
        public String dataInicio;
        /** yyyy-MM-dd ou null */
        public String dataFim;
        public String safraUuid;
        public String propriedadeUuid;
        public String talhaoUuid;
        public String maquinaUuid;
        /** Nome exato da categoria, ou null. */
        public String categoria;
        /** Ex.: "Soja" — filtra todas as safras/talhões dessa cultura. */
        public String cultura;
        /** SAFRAS: ATIVAS | ENCERRADAS | TODAS (null = ATIVAS) */
        public String situacao;
        /** CATEGORIA | SAFRA | MES | MAQUINA | null */
        public String agrupar;
    }
}
