package com.tcc.gestaorural.data.local.model;

/** Despesa ou receita com os dados de safra/talhão/máquina, para as consultas do assistente de IA. */
public class LancamentoLinha {
    private String tipo;             // "DESPESA" ou "RECEITA"
    private String data;             // yyyy-MM-dd
    private double valor;
    private String descricao;
    private String categoria;        // só despesa
    private String safraUuid;
    private String cultura;
    private String anoSafra;
    private String talhaoNome;
    private String talhaoUuid;
    private String propriedadeUuid;
    private String maquinaUuid;      // só despesa (opcional)
    private String maquinaNome;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getSafraUuid() { return safraUuid; }
    public void setSafraUuid(String safraUuid) { this.safraUuid = safraUuid; }
    public String getCultura() { return cultura; }
    public void setCultura(String cultura) { this.cultura = cultura; }
    public String getAnoSafra() { return anoSafra; }
    public void setAnoSafra(String anoSafra) { this.anoSafra = anoSafra; }
    public String getTalhaoNome() { return talhaoNome; }
    public void setTalhaoNome(String talhaoNome) { this.talhaoNome = talhaoNome; }
    public String getTalhaoUuid() { return talhaoUuid; }
    public void setTalhaoUuid(String talhaoUuid) { this.talhaoUuid = talhaoUuid; }
    public String getPropriedadeUuid() { return propriedadeUuid; }
    public void setPropriedadeUuid(String propriedadeUuid) { this.propriedadeUuid = propriedadeUuid; }
    public String getMaquinaUuid() { return maquinaUuid; }
    public void setMaquinaUuid(String maquinaUuid) { this.maquinaUuid = maquinaUuid; }
    public String getMaquinaNome() { return maquinaNome; }
    public void setMaquinaNome(String maquinaNome) { this.maquinaNome = maquinaNome; }
}
