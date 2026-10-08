package com.tcc.gestaorural.data.remote.dto;

import java.util.List;

/** GET /api/equipe/minha (e resposta de aceitar convite / sair da equipe): papel e equipe de quem está logado. */
public class EquipeMinhaResponseDto {
    private String papel;
    private Dono dono;
    private List<Pessoa> pessoas;
    private Integer convitesPendentes;

    /** Dono da fazenda (null quando quem está logado é o próprio dono). Contrato: {usuarioUuid, nome, papel}. */
    public static class Dono {
        private String usuarioUuid;
        /** Nome antigo do campo (versão inicial do contrato); lido só por compatibilidade. */
        private String uuid;
        private String nome;
        private String papel;

        /** uuid do usuário dono (aceita os dois nomes do campo). */
        public String getUuid() { return usuarioUuid != null ? usuarioUuid : uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getUsuarioUuid() { return usuarioUuid; }
        public void setUsuarioUuid(String usuarioUuid) { this.usuarioUuid = usuarioUuid; }
        public String getPapel() { return papel; }
        public void setPapel(String papel) { this.papel = papel; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
    }

    /** Dono + membros aceitos — lista usada pra escolher o responsável de uma tarefa. */
    public static class Pessoa {
        private String usuarioUuid;
        private String nome;
        private String papel;

        public String getUsuarioUuid() { return usuarioUuid; }
        public void setUsuarioUuid(String usuarioUuid) { this.usuarioUuid = usuarioUuid; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getPapel() { return papel; }
        public void setPapel(String papel) { this.papel = papel; }
    }

    public String getPapel() { return papel; }
    public void setPapel(String papel) { this.papel = papel; }
    public Dono getDono() { return dono; }
    public void setDono(Dono dono) { this.dono = dono; }
    public List<Pessoa> getPessoas() { return pessoas; }
    public void setPessoas(List<Pessoa> pessoas) { this.pessoas = pessoas; }
    public Integer getConvitesPendentes() { return convitesPendentes; }
    public void setConvitesPendentes(Integer convitesPendentes) { this.convitesPendentes = convitesPendentes; }
}
