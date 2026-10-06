package com.tcc.gestaorural.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferências simples das configurações (persistidas). Também guarda os
 *  marcadores de "última sincronização" usados pelo SyncManager. */
public class SettingsManager {
    private static final String PREFS = "gestao_rural_config";
    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean get(String chave, boolean padrao) { return prefs.getBoolean(chave, padrao); }
    public void set(String chave, boolean valor) { prefs.edit().putBoolean(chave, valor).apply(); }

    public String get(String chave, String padrao) { return prefs.getString(chave, padrao); }
    public void set(String chave, String valor) { prefs.edit().putString(chave, valor).apply(); }

    public void remover(String chave) { prefs.edit().remove(chave).commit(); }
}
