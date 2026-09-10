package com.tcc.gestaorural;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.util.SettingsManager;

public class GestaoRuralApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Aplica o modo claro/escuro conforme a preferência salva (padrão: claro).
        boolean escuro = new SettingsManager(this).get("modo_escuro", false);
        AppCompatDelegate.setDefaultNightMode(
                escuro ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        // Inicializa o banco (dispara o seed de categorias na 1ª execução).
        AppDatabase.getInstance(this);
    }
}
