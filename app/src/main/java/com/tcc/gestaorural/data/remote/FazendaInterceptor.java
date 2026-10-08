package com.tcc.gestaorural.data.remote;

import android.content.Context;

import com.tcc.gestaorural.util.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Proteção da fazenda na sincronização (08/10/2026, combinado com o backend).
 *
 * Em toda chamada de sync (GET/POST /api/{entidade}/sync) manda o cabeçalho
 * X-SafraPay-Fazenda com de quem são os dados que estão no aparelho: "PROPRIA" ou o uuid do
 * dono da equipe (o mesmo valor de SessionManager.getFazendaCarregada()). Se a situação na
 * equipe mudou sem o aparelho saber (ex.: gerente removido enquanto estava offline), o
 * servidor responde 409 com X-SafraPay-Erro: FAZENDA e não grava nada — aí o papel é marcado
 * como desatualizado e a próxima tela confere /api/equipe/minha e troca os dados do aparelho.
 */
public class FazendaInterceptor implements Interceptor {

    private final Context appContext;

    public FazendaInterceptor(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request req = chain.request();
        if (!req.url().encodedPath().endsWith("/sync")) return chain.proceed(req);

        SessionManager s = new SessionManager(appContext);
        String fazenda = s.getFazendaCarregada();
        if (fazenda != null) req = req.newBuilder().header("X-SafraPay-Fazenda", fazenda).build();

        Response resposta = chain.proceed(req);
        if (resposta.code() == 409 && "FAZENDA".equalsIgnoreCase(resposta.header("X-SafraPay-Erro"))) {
            s.marcarPapelDesatualizado();
        }
        return resposta;
    }
}
