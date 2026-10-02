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
    private String status;
    /** Texto pra mostrar na bolha da IA (na CONSULTA, só uma introdução sem números). */
    private String resposta;
    private List<Lancamento> lancamentos = new ArrayList<>();
    /** Quantas mensagens o usuário ainda pode mandar hoje (limite diário de custo). */
    private Integer mensagensRestantesHoje;
    /** v2: só quando status = CONSULTA. */
    private Consulta consulta;
    /** v2: respostas rápidas (o app mostra como botões; tocar envia o texto). */
    private List<String> sugestoes;
    /** Avisos do servidor (ex.: arquivo grande cortado). */
    private List<String> avisos;

    public static class Lancamento {
        /** "DESPESA" ou "RECEITA". */
        private String tipo;
        private Double valor;
        /** yyyy-MM-dd */
        private String data;
        private String descricao;
        /** Nome exato de uma categoria do contexto (só despesa). */
        private String categoria;
        /** uuid de uma safra do contexto. */
        private String safraUuid;
        /** uuid de uma máquina do contexto (opcional, só despesa). */
        private String maquinaUuid;
        /** Campos que faltam pra gravar. Vazio = completo. */
        private List<String> camposFaltando = new ArrayList<>();

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public Double getValor() { return valor; }
        public void setValor(Double valor) { this.valor = valor; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public String getSafraUuid() { return safraUuid; }
        public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
        public String getMaquinaUuid() { return maquinaUuid; }
        public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
        public List<String> getCamposFaltando() { return camposFaltando; }
        public void setCamposFaltando(List<String> camposFaltando) { this.camposFaltando = camposFaltando; }
    }

    /** v2: pergunta estruturada que o app executa no Room. */
    public static class Consulta {
        /** GASTOS | RECEITAS | SALDO | LANCAMENTOS | SAFRAS | AREA | METAS | MAQUINAS | COMPROVANTE (v3) */
        private String tipo;
        /** yyyy-MM-dd ou null */
        private String dataInicio;
        /** yyyy-MM-dd ou null */
        private String dataFim;
        private String safraUuid;
        private String propriedadeUuid;
        private String talhaoUuid;
        private String maquinaUuid;
        /** Nome exato da categoria, ou null. */
        private String categoria;
        /** Ex.: "Soja" — filtra todas as safras/talhões dessa cultura. */
        private String cultura;
        /** SAFRAS: ATIVAS | ENCERRADAS | TODAS (null = ATIVAS) */
        private String situacao;
        /** CATEGORIA | SAFRA | MES | MAQUINA | null */
        private String agrupar;
        /** v3: palavra-chave procurada na descrição do lançamento (sem diferenciar maiúsculas/acentos). */
        private String texto;
        /** v3: valor citado pelo produtor ("a nota de 3 mil" → 3000); comparado com tolerância de R$ 0,01. */
        private Double valor;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getDataInicio() { return dataInicio; }
        public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }
        public String getDataFim() { return dataFim; }
        public void setDataFim(String dataFim) { this.dataFim = dataFim; }
        public String getSafraUuid() { return safraUuid; }
        public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
        public String getPropriedadeUuid() { return propriedadeUuid; }
        public void setPropriedadeUuid(String propriedadeUuid) { this.propriedadeUuid = propriedadeUuid; }
        public String getTalhaoUuid() { return talhaoUuid; }
        public void setTalhaoUuid(String talhaoUuid) { this.talhaoUuid = talhaoUuid; }
        public String getMaquinaUuid() { return maquinaUuid; }
        public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public String getCultura() { return cultura; }
        public void setCultura(String cultura) { this.cultura = cultura; }
        public String getSituacao() { return situacao; }
        public void setSituacao(String situacao) { this.situacao = situacao; }
        public String getAgrupar() { return agrupar; }
        public void setAgrupar(String agrupar) { this.agrupar = agrupar; }
        public String getTexto() { return texto; }
        public void setTexto(String texto) { this.texto = texto; }
        public Double getValor() { return valor; }
        public void setValor(Double valor) { this.valor = valor; }
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResposta() { return resposta; }
    public void setResposta(String resposta) { this.resposta = resposta; }
    public List<Lancamento> getLancamentos() { return lancamentos; }
    public void setLancamentos(List<Lancamento> lancamentos) { this.lancamentos = lancamentos; }
    public Integer getMensagensRestantesHoje() { return mensagensRestantesHoje; }
    public void setMensagensRestantesHoje(Integer mensagensRestantesHoje) { this.mensagensRestantesHoje = mensagensRestantesHoje; }
    public Consulta getConsulta() { return consulta; }
    public void setConsulta(Consulta consulta) { this.consulta = consulta; }
    public List<String> getSugestoes() { return sugestoes; }
    public void setSugestoes(List<String> sugestoes) { this.sugestoes = sugestoes; }
    public List<String> getAvisos() { return avisos; }
    public void setAvisos(List<String> avisos) { this.avisos = avisos; }
}
