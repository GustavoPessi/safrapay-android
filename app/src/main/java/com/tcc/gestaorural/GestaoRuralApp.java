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
        // Cliente da API com renovação automática do token (refresh token).
        com.tcc.gestaorural.data.remote.RetrofitClient.init(this);
        // Faxina dos comprovantes cujo lançamento foi apagado (ex.: safra excluída em cascata).
        // Só com os dados da própria conta completos no aparelho: em equipe (ou no meio de uma
        // troca de conta) os lançamentos próprios não estão no Room e os comprovantes deles
        // precisam ficar guardados pra quando os dados voltarem.
        com.tcc.gestaorural.util.SessionManager sessao = new com.tcc.gestaorural.util.SessionManager(this);
        if (sessao.ehDono() && !sessao.pullCompletoPendente()) {
            new com.tcc.gestaorural.data.repository.AnexoRepository(this).limparOrfaos();
        }
    }
}
