package com.tcc.gestaorural.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.dashboard.DashboardActivity;
import com.tcc.gestaorural.ui.auth.LoginActivity;
import com.tcc.gestaorural.ui.onboarding.OnboardingActivity;
import com.tcc.gestaorural.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long DELAY_MS = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::decidirRota, DELAY_MS);
    }

    private void decidirRota() {
        SessionManager session = new SessionManager(this);
        Intent destino;
        if (session.estaLogado()) {
            // Colaborador abre direto no app dele (papel guardado; o Início/tela dele confere de novo online).
            destino = new Intent(this, com.tcc.gestaorural.ui.equipe.EquipeSincronizador.telaInicial(session));
        } else if (!session.onboardingVisto()) {
            destino = new Intent(this, OnboardingActivity.class);
        } else {
            destino = new Intent(this, LoginActivity.class);
        }
        startActivity(destino);
        finish();
    }
}
