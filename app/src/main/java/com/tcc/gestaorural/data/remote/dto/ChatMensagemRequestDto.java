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

    public List<Mensagem> mensagens;
    /** Data de hoje no aparelho (yyyy-MM-dd), pra IA resolver "ontem", "09/08" (ano atual) etc. */
    public String hoje;
    public Contexto contexto;
    /** Foto opcional (nota fiscal, cupom, recibo) — vai com a ÚLTIMA mensagem. Não pode vir junto com arquivo. */
    public Imagem imagem;
    /** Arquivo opcional (.xlsx, .csv ou .txt com vários lançamentos) — vai com a ÚLTIMA mensagem. */
    public Arquivo arquivo;

    public static class Imagem {
        /** image/jpeg, image/png ou image/webp (o servidor confere pelos bytes). Até 5 MB. */
        public String tipo;
        public String base64;

        public Imagem() {}
        public Imagem(String tipo, String base64) { this.tipo = tipo; this.base64 = base64; }
    }

    public static class Arquivo {
        /** Nome original com extensão (ex.: "gastos-setembro.xlsx"). Até 2 MB. */
        public String nome;
        public String base64;

        public Arquivo() {}
        public Arquivo(String nome, String base64) { this.nome = nome; this.base64 = base64; }
    }

    public static class Mensagem {
        /** "usuario" ou "assistente". */
        public String papel;
        public String texto;

        public Mensagem() {}
        public Mensagem(String papel, String texto) { this.papel = papel; this.texto = texto; }
    }

    public static class Contexto {
        public List<Safra> safras;
        public List<String> categorias;
        public List<Maquina> maquinas;
        /** v2 */
        public List<PropriedadeCtx> propriedades;
        /** v2 */
        public List<TalhaoCtx> talhoes;
    }

    public static class Safra {
        public String uuid;
        /** Ex.: "Soja • 2025/2026 • Talhão Ricardo" */
        public String descricao;
        /** true = safra sem data de fim (em andamento). */
        public Boolean ativa;
        // --- v2 ---
        public String cultura;
        public String talhao;
        public String anoSafra;
        /** yyyy-MM-dd ou null */
        public String dataInicio;
        /** yyyy-MM-dd ou null (null = em andamento) */
        public String dataFim;
        public String propriedadeUuid;
    }

    public static class Maquina {
        public String uuid;
        public String nome;

        public Maquina() {}
        public Maquina(String uuid, String nome) { this.uuid = uuid; this.nome = nome; }
    }

    /** v2 */
    public static class PropriedadeCtx {
        public String uuid;
        public String nome;
        /** hectares, pode ser null */
        public Double areaTotal;
    }

    /** v2 */
    public static class TalhaoCtx {
        public String uuid;
        public String nome;
        public String propriedadeUuid;
        public String cultura;
        /** hectares, pode ser null */
        public Double areaHa;
    }
}
