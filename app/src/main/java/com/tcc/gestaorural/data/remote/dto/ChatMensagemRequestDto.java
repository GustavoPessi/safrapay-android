package com.tcc.gestaorural.data.remote.dto;

import java.util.List;

/**
 * Corpo do POST /api/chat/mensagem (assistente com IA). Espelha o
 * ChatMensagemRequest do backend. O servidor não guarda a conversa: a cada
 * chamada o app manda o histórico recente (a última é a nova mensagem do
 * produtor) + o contexto do usuário, com os uuids do Room (inclusive coisas
 * cadastradas offline que ainda não sincronizaram).
 *
 * Contrato v2 (01/10/2026, ver Notion "Contrato do Assistente de IA — v2"):
 * contexto ampliado com datas das safras, propriedades e talhões. Nenhum valor
 * financeiro vai no contexto — perguntas sobre dinheiro são calculadas no app.
 */
public class ChatMensagemRequestDto {

    private List<Mensagem> mensagens;
    /** Data de hoje no aparelho (yyyy-MM-dd), pra IA resolver "ontem", "09/08" (ano atual) etc. */
    private String hoje;
    private Contexto contexto;
    /** Foto opcional (nota fiscal, cupom, recibo) — vai com a ÚLTIMA mensagem. Não pode vir junto com arquivo. */
    private Imagem imagem;
    /** Arquivo opcional (.xlsx, .csv ou .txt com vários lançamentos) — vai com a ÚLTIMA mensagem. */
    private Arquivo arquivo;

    public static class Imagem {
        /** image/jpeg, image/png ou image/webp (o servidor confere pelos bytes). Até 5 MB. */
        private String tipo;
        private String base64;

        public Imagem() {}
        public Imagem(String tipo, String base64) { this.tipo = tipo; this.base64 = base64; }

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getBase64() { return base64; }
        public void setBase64(String base64) { this.base64 = base64; }
    }

    public static class Arquivo {
        /** Nome original com extensão (ex.: "gastos-setembro.xlsx"). Até 2 MB. */
        private String nome;
        private String base64;

        public Arquivo() {}
        public Arquivo(String nome, String base64) { this.nome = nome; this.base64 = base64; }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getBase64() { return base64; }
        public void setBase64(String base64) { this.base64 = base64; }
    }

    public static class Mensagem {
        /** "usuario" ou "assistente". */
        private String papel;
        private String texto;

        public Mensagem() {}
        public Mensagem(String papel, String texto) { this.papel = papel; this.texto = texto; }

        public String getPapel() { return papel; }
        public void setPapel(String papel) { this.papel = papel; }
        public String getTexto() { return texto; }
        public void setTexto(String texto) { this.texto = texto; }
    }

    public static class Contexto {
        private List<Safra> safras;
        private List<String> categorias;
        private List<Maquina> maquinas;
        /** v2 */
        private List<PropriedadeCtx> propriedades;
        /** v2 */
        private List<TalhaoCtx> talhoes;

        public List<Safra> getSafras() { return safras; }
        public void setSafras(List<Safra> safras) { this.safras = safras; }
        public List<String> getCategorias() { return categorias; }
        public void setCategorias(List<String> categorias) { this.categorias = categorias; }
        public List<Maquina> getMaquinas() { return maquinas; }
        public void setMaquinas(List<Maquina> maquinas) { this.maquinas = maquinas; }
        public List<PropriedadeCtx> getPropriedades() { return propriedades; }
        public void setPropriedades(List<PropriedadeCtx> propriedades) { this.propriedades = propriedades; }
        public List<TalhaoCtx> getTalhoes() { return talhoes; }
        public void setTalhoes(List<TalhaoCtx> talhoes) { this.talhoes = talhoes; }
    }

    public static class Safra {
        private String uuid;
        /** Ex.: "Soja • 2025/2026 • Talhão Ricardo" */
        private String descricao;
        /** true = safra sem data de fim (em andamento). */
        private Boolean ativa;
        // --- v2 ---
        private String cultura;
        private String talhao;
        private String anoSafra;
        /** yyyy-MM-dd ou null */
        private String dataInicio;
        /** yyyy-MM-dd ou null (null = em andamento) */
        private String dataFim;
        private String propriedadeUuid;

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public Boolean getAtiva() { return ativa; }
        public void setAtiva(Boolean ativa) { this.ativa = ativa; }
        public String getCultura() { return cultura; }
        public void setCultura(String cultura) { this.cultura = cultura; }
        public String getTalhao() { return talhao; }
        public void setTalhao(String talhao) { this.talhao = talhao; }
        public String getAnoSafra() { return anoSafra; }
        public void setAnoSafra(String anoSafra) { this.anoSafra = anoSafra; }
        public String getDataInicio() { return dataInicio; }
        public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }
        public String getDataFim() { return dataFim; }
        public void setDataFim(String dataFim) { this.dataFim = dataFim; }
        public String getPropriedadeUuid() { return propriedadeUuid; }
        public void setPropriedadeUuid(String propriedadeUuid) { this.propriedadeUuid = propriedadeUuid; }
    }

    public static class Maquina {
        private String uuid;
        private String nome;

        public Maquina() {}
        public Maquina(String uuid, String nome) { this.uuid = uuid; this.nome = nome; }

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
    }

    /** v2 */
    public static class PropriedadeCtx {
        private String uuid;
        private String nome;
        /** hectares, pode ser null */
        private Double areaTotal;

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public Double getAreaTotal() { return areaTotal; }
        public void setAreaTotal(Double areaTotal) { this.areaTotal = areaTotal; }
    }

    /** v2 */
    public static class TalhaoCtx {
        private String uuid;
        private String nome;
        private String propriedadeUuid;
        private String cultura;
        /** hectares, pode ser null */
        private Double areaHa;

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getPropriedadeUuid() { return propriedadeUuid; }
        public void setPropriedadeUuid(String propriedadeUuid) { this.propriedadeUuid = propriedadeUuid; }
        public String getCultura() { return cultura; }
        public void setCultura(String cultura) { this.cultura = cultura; }
        public Double getAreaHa() { return areaHa; }
        public void setAreaHa(Double areaHa) { this.areaHa = areaHa; }
    }

    public List<Mensagem> getMensagens() { return mensagens; }
    public void setMensagens(List<Mensagem> mensagens) { this.mensagens = mensagens; }
    public String getHoje() { return hoje; }
    public void setHoje(String hoje) { this.hoje = hoje; }
    public Contexto getContexto() { return contexto; }
    public void setContexto(Contexto contexto) { this.contexto = contexto; }
    public Imagem getImagem() { return imagem; }
    public void setImagem(Imagem imagem) { this.imagem = imagem; }
    public Arquivo getArquivo() { return arquivo; }
    public void setArquivo(Arquivo arquivo) { this.arquivo = arquivo; }
}
