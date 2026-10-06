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
    // Multiusuário (06/10/2026)
    private static final String KEY_PAPEL = "equipe_papel";
    private static final String KEY_DONO_UUID = "equipe_dono_uuid";
    private static final String KEY_DONO_NOME = "equipe_dono_nome";
    private static final String KEY_PESSOAS = "equipe_pessoas_json";
    private static final String KEY_CONVITES = "equipe_convites_pendentes";
    private static final String KEY_FAZENDA = "equipe_fazenda_carregada";
    private static final String KEY_PULL_PENDENTE = "equipe_pull_completo_pendente";
    private static final String KEY_PAPEL_DESATUALIZADO = "equipe_papel_desatualizado";

    public static final String PAPEL_DONO = "DONO";
    public static final String PAPEL_GERENTE = "GERENTE";
    public static final String PAPEL_COLABORADOR = "COLABORADOR";
    /** Valor de "fazenda carregada" quando os dados no aparelho são da própria conta. */
    public static final String FAZENDA_PROPRIA = "PROPRIA";

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
                .remove(KEY_SESSAO_ENCERRADA)
                .remove(KEY_PAPEL).remove(KEY_DONO_UUID).remove(KEY_DONO_NOME).remove(KEY_PESSOAS)
                .remove(KEY_CONVITES).apply();
        // KEY_FAZENDA e KEY_PULL_PENDENTE ficam: dizem de quem são os dados que estão no aparelho.
    }

    // --- Multiusuário ---------------------------------------------------

    /** DONO (padrão: quem não está em equipe), GERENTE ou COLABORADOR. */
    public String getPapel() { return prefs.getString(KEY_PAPEL, PAPEL_DONO); }
    public boolean ehDono() { return PAPEL_DONO.equals(getPapel()); }
    public boolean ehGerente() { return PAPEL_GERENTE.equals(getPapel()); }
    public boolean ehColaborador() { return PAPEL_COLABORADOR.equals(getPapel()); }
    public String getDonoUuid() { return prefs.getString(KEY_DONO_UUID, null); }
    public String getDonoNome() { return prefs.getString(KEY_DONO_NOME, null); }
    /** JSON da lista "pessoas" de /api/equipe/minha (pra escolher responsável de tarefa). */
    public String getPessoasJson() { return prefs.getString(KEY_PESSOAS, null); }
    public int getConvitesPendentes() { return prefs.getInt(KEY_CONVITES, 0); }

    public void salvarEquipe(String papel, String donoUuid, String donoNome, String pessoasJson, int convitesPendentes) {
        prefs.edit().putString(KEY_PAPEL, papel == null ? PAPEL_DONO : papel)
                .putString(KEY_DONO_UUID, donoUuid).putString(KEY_DONO_NOME, donoNome)
                .putString(KEY_PESSOAS, pessoasJson).putInt(KEY_CONVITES, convitesPendentes).commit();
    }

    public void setConvitesPendentes(int n) { prefs.edit().putInt(KEY_CONVITES, n).apply(); }

    /** O servidor recusou uma ação por causa do papel (403): conferir o papel de novo. */
    public void marcarPapelDesatualizado() { prefs.edit().putBoolean(KEY_PAPEL_DESATUALIZADO, true).apply(); }
    public boolean papelDesatualizado() { return prefs.getBoolean(KEY_PAPEL_DESATUALIZADO, false); }
    public void limparPapelDesatualizado() { prefs.edit().remove(KEY_PAPEL_DESATUALIZADO).apply(); }

    /**
     * De quem são os dados que estão no Room: FAZENDA_PROPRIA ou o uuid do dono da equipe.
     * null = ainda não sabemos (app atualizado agora): tratado como próprio.
     */
    public String getFazendaCarregada() { return prefs.getString(KEY_FAZENDA + "_" + getUsuarioId(), null); }
    public void setFazendaCarregada(String fazenda) { prefs.edit().putString(KEY_FAZENDA + "_" + getUsuarioId(), fazenda).commit(); }

    /** true entre limpar o Room e terminar o pull completo da conta nova. */
    public boolean pullCompletoPendente() { return prefs.getBoolean(KEY_PULL_PENDENTE + "_" + getUsuarioId(), false); }
    public void setPullCompletoPendente(boolean pendente) { prefs.edit().putBoolean(KEY_PULL_PENDENTE + "_" + getUsuarioId(), pendente).commit(); }

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
