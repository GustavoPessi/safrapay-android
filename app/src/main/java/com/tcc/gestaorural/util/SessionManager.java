package com.tcc.gestaorural.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda o usuário logado e se o onboarding já foi visto. */
public class SessionManager {
    private static final String PREFS = "gestao_rural_prefs";
    private static final String KEY_USER_ID = "usuario_id";
    private static final String KEY_ONBOARDING = "onboarding_visto";
    private static final String KEY_TOKEN = "jwt_token";

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
    public void logout() { prefs.edit().remove(KEY_USER_ID).remove(KEY_TOKEN).apply(); }

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
