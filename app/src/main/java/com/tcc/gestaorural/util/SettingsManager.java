package com.tcc.gestaorural.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferências simples das configurações (persistidas). */
public class SettingsManager {
    private static final String PREFS = "gestao_rural_config";
    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean get(String chave, boolean padrao) { return prefs.getBoolean(chave, padrao); }
    public void set(String chave, boolean valor) { prefs.edit().putBoolean(chave, valor).apply(); }
}
