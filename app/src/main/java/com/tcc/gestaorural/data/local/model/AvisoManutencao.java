package com.tcc.gestaorural.data.local.model;

/** Anotação de máquina pendente próxima/vencida (para a tela de Notificações). */
public class AvisoManutencao {
    public long id;
    public long maquinaId;
    public String texto;
    public String data;          // ISO
    public String maquinaNome;
}
