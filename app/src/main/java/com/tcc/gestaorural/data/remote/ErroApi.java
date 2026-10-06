package com.tcc.gestaorural.data.remote;

import retrofit2.Response;

/** Lê a mensagem de erro que o servidor mandou no corpo ("mensagem" ou "message" do Spring). */
public final class ErroApi {

    private ErroApi() {}

    public static String mensagem(Response<?> r, String padrao) {
        try {
            if (r.errorBody() == null) return padrao;
            String corpo = r.errorBody().string();
            if (corpo == null || corpo.trim().isEmpty()) return padrao;
            org.json.JSONObject json = new org.json.JSONObject(corpo);
            String m = json.optString("mensagem", "");
            if (m.isEmpty()) m = json.optString("message", "");
            return m.isEmpty() ? padrao : m;
        } catch (Exception e) {
            return padrao;
        }
    }
}
