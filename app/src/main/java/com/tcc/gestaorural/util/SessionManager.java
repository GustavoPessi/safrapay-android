package com.tcc.gestaorural.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda o usuário logado e se o onboarding já foi visto. */
public class SessionManager {
    private static final String PREFS = "gestao_rural_prefs";
    private static final String KEY_USER_ID = "usuario_id";
    private static final String KEY_ONBOARDING = "onboarding_visto";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_SESSAO_ENCERRADA = "sessao_encerrada";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void salvarLogin(long usuarioId) {
        prefs.edit().putLong(KEY_USER_ID, usuarioId).apply();
    }

    public long getUsuarioId() { return prefs.getLong(KEY_USER_ID, -1L); }
    public boolean estaLogado() { return getUsuarioId() != -1L; }
    public void logout() {
        prefs.edit().remove(KEY_USER_ID).remove(KEY_TOKEN).remove(KEY_REFRESH)
                .remove(KEY_SESSAO_ENCERRADA).apply();
    }

    /**
     * Refresh token (backend 01/10/2026): vale 90 dias e muda a cada renovação —
     * sempre guardar o último. Usado pra pegar um token de acesso novo quando o
     * de 24h vence, sem o produtor digitar a senha de novo.
     */
    public void salvarRefreshToken(String refreshToken) {
        prefs.edit().putString(KEY_REFRESH, refreshToken).apply();
    }
    public String getRefreshToken() { return prefs.getString(KEY_REFRESH, null); }

    /** Salva o par recebido do login/refresh (commit síncrono: quem chama pode estar numa thread de rede). */
    public synchronized void salvarTokens(String token, String refreshToken) {
        SharedPreferences.Editor e = prefs.edit().putString(KEY_TOKEN, token).remove(KEY_SESSAO_ENCERRADA);
        if (refreshToken != null) e.putString(KEY_REFRESH, refreshToken);
        e.commit();
    }

    /** O servidor recusou o refresh (401): a sessão acabou de verdade, precisa logar de novo. */
    public void marcarSessaoEncerrada() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_REFRESH).putBoolean(KEY_SESSAO_ENCERRADA, true).commit();
    }
    public boolean sessaoEncerrada() { return prefs.getBoolean(KEY_SESSAO_ENCERRADA, false); }

    /** Token JWT do backend (fase de testes - ainda nao usado por outras chamadas). */
    public void salvarToken(String token) { prefs.edit().putString(KEY_TOKEN, token).apply(); }
    public String getToken() { return prefs.getString(KEY_TOKEN, null); }

    /**
     * true se existe token e ele ainda não venceu. O backend emite JWT com validade
     * de 24h; aqui só lemos o campo "exp" do payload (sem validar assinatura — quem
     * valida é o servidor) pra avisar o usuário antes de mandar um pedido que vai dar 403.
     */
    public boolean tokenValido() {
        String t = getToken();
        if (t == null || t.isEmpty()) return false;
        try {
            String[] partes = t.split("\\.");
            if (partes.length < 2) return false;
            byte[] bytes = android.util.Base64.decode(partes[1],
                    android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP);
            org.json.JSONObject payload = new org.json.JSONObject(new String(bytes, "UTF-8"));
            long exp = payload.optLong("exp", 0L);
            if (exp == 0L) return true;
            return (System.currentTimeMillis() / 1000L) < (exp - 60L); // 1 min de margem
        } catch (Exception e) {
            return true; // não deu pra ler: deixa o servidor decidir
        }
    }

    public boolean onboardingVisto() { return prefs.getBoolean(KEY_ONBOARDING, false); }
    public void marcarOnboardingVisto() {
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply();
    }
}
