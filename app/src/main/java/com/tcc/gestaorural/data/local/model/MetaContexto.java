package com.tcc.gestaorural.data.local.model;

/** Meta financeira com a safra vinculada, para a consulta METAS do assistente de IA. */
public class MetaContexto {
    private String descricao;
    private double valorAlvo;
    private String prazo;            // yyyy-MM-dd (pode ser null)
    private String safraUuid;
    private String cultura;
    private String anoSafra;
    private String talhaoNome;

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public double getValorAlvo() { return valorAlvo; }
    public void setValorAlvo(double valorAlvo) { this.valorAlvo = valorAlvo; }
    public String getPrazo() { return prazo; }
    public void setPrazo(String prazo) { this.prazo = prazo; }
    public String getSafraUuid() { return safraUuid; }
    public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
    public String getCultura() { return cultura; }
    public void setCultura(String cultura) { this.cultura = cultura; }
    public String getAnoSafra() { return anoSafra; }
    public void setAnoSafra(String anoSafra) { this.anoSafra = anoSafra; }
    public String getTalhaoNome() { return talhaoNome; }
    public void setTalhaoNome(String talhaoNome) { this.talhaoNome = talhaoNome; }
}
