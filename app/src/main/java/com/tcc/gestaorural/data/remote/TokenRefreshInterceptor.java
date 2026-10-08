package com.tcc.gestaorural.data.remote;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Se uma chamada autenticada (com "Authorization: Bearer ...") voltar 401 ou 403,
 * renova o token pelo refresh token e repete a MESMA chamada uma vez com o token novo.
 * Rotas públicas (login, refresh, logout...) não mandam o header, então não entram aqui.
 */
public class TokenRefreshInterceptor implements Interceptor {

    private final Context appContext;

    public TokenRefreshInterceptor(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Response resposta = chain.proceed(original);

        String auth = original.header("Authorization");
        boolean negado = resposta.code() == 401 || resposta.code() == 403;
        if (!negado || auth == null || !auth.startsWith("Bearer ")) return resposta;

        // 403 de PAPEL (o servidor marca com o cabeçalho X-SafraPay-Erro: PAPEL — ex.: colaborador
        // numa rota de dono): não é token vencido, não adianta renovar. Quem chamou trata e a
        // próxima tela confere o papel de novo. Sem o cabeçalho, 403 continua sendo sessão vencida;
        // por segurança, 403 com token ainda válido também não renova.
        String usado = auth.substring("Bearer ".length());
        if (resposta.code() == 403) {
            com.tcc.gestaorural.util.SessionManager s = new com.tcc.gestaorural.util.SessionManager(appContext);
            boolean erroDePapel = "PAPEL".equalsIgnoreCase(resposta.header("X-SafraPay-Erro"));
            if (erroDePapel || (usado.equals(s.getToken()) && s.tokenValido())) {
                s.marcarPapelDesatualizado(); // a tela confere /api/equipe/minha e se ajusta
                return resposta;
            }
        }

        String novo = TokenRefresher.renovar(appContext, usado);
        if (novo == null) return resposta; // não renovou: quem chamou trata o 401/403

        resposta.close();
        return chain.proceed(original.newBuilder().header("Authorization", "Bearer " + novo).build());
    }
}
