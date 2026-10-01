package com.tcc.gestaorural.data.remote.dto;

import java.util.List;

/**
 * Corpo do POST /api/chat/mensagem (assistente de lançamentos com IA).
 * Espelha o ChatMensagemRequest do backend. O servidor não guarda a conversa:
 * a cada chamada o app manda o histórico recente (a última mensagem é a nova do
 * produtor) + o contexto do usuário (safras/categorias/máquinas com os uuids do
 * Room, inclusive coisas cadastradas offline que ainda não sincronizaram).
 */
public class ChatMensagemRequestDto {

    public List<Mensagem> mensagens;
    /** Data de hoje no aparelho (yyyy-MM-dd), pra IA resolver "ontem", "sexta passada"... */
    public String hoje;
    public Contexto contexto;

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
    }

    public static class Safra {
        public String uuid;
        /** Ex.: "Soja • 2025/2026 • Talhão Ricardo" */
        public String descricao;
        /** true = safra sem data de fim (em andamento). */
        public Boolean ativa;

        public Safra() {}
        public Safra(String uuid, String descricao, Boolean ativa) {
            this.uuid = uuid; this.descricao = descricao; this.ativa = ativa;
        }
    }

    public static class Maquina {
        public String uuid;
        public String nome;

        public Maquina() {}
        public Maquina(String uuid, String nome) { this.uuid = uuid; this.nome = nome; }
    }
}
