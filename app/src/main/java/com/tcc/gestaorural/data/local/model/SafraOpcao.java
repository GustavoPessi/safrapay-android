package com.tcc.gestaorural.data.local.model;

/** Projeção usada no seletor de safra (JOIN safra + talhão). Cultura vem do talhão. */
public class SafraOpcao {
    public long id;
    public String cultura;       // = talhao.cultura
    public String anoSafra;
    public String talhaoNome;

    /** Rótulo exibido: "Talhão A • Soja • 2025/26". */
    public String rotulo() {
        String c = (cultura == null || cultura.trim().isEmpty()) ? "Sem cultura" : cultura;
        return talhaoNome + " • " + c + " • " + anoSafra;
    }
}
