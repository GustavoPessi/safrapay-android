package com.tcc.gestaorural.data.remote.dto;

/**
 * Aviso de exclusão trocado em POST/GET /api/exclusoes/sync (08/10/2026).
 * "entidade": propriedade, talhao, safra, receita, despesa, meta, maquina ou anotacao_maquina.
 * "excluidoEm": instante UTC ISO (no pull é o marcador "desde" da próxima vez).
 */
public class ExclusaoSyncItemDto {
    private String entidade;
    private String uuid;
    private String excluidoEm;

    public ExclusaoSyncItemDto() {}

    public ExclusaoSyncItemDto(String entidade, String uuid, String excluidoEm) {
        this.entidade = entidade;
        this.uuid = uuid;
        this.excluidoEm = excluidoEm;
    }

    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getExcluidoEm() { return excluidoEm; }
    public void setExcluidoEm(String excluidoEm) { this.excluidoEm = excluidoEm; }
}
