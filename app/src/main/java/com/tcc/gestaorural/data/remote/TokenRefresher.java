package com.tcc.gestaorural.data.remote;

import android.content.Context;

import com.tcc.gestaorural.data.remote.dto.AutenticacaoResponseDto;
import com.tcc.gestaorural.data.remote.dto.RefreshRequestDto;
import com.tcc.gestaorural.util.SessionManager;

import java.io.IOException;

import retrofit2.Response;

/**
 * Renovação do token de acesso (JWT de 24h) usando o refresh token (90 dias).
 *
 * Regras do contrato com o backend (Notion, "Contrato do Assistente de IA — v2", seção 9):
 * - Um refresh só, mesmo se várias chamadas derem 403 juntas (sync + chat): o bloqueio
 *   abaixo serializa, e quem chega depois reaproveita o token que o primeiro já renovou.
 * - 200 → salvar token E refreshToken novos (o refresh muda a cada troca).
 * - 401 → sessão encerrada de verdade: limpar os tokens e mandar pro login.
 * - Sem rede/erro do servidor → não limpa nada; tenta de novo na próxima chamada
 *   (o servidor aceita repetir com o refresh antigo por até 2 min).
 */
public final class TokenRefresher {

    private static final Object LOCK = new Object();

    private TokenRefresher() {}

    /** Devolve um token de acesso pra repetir a chamada, ou null se não deu pra renovar. */
    static String renovar(Context context, String tokenUsadoNaChamada) {
        synchronized (LOCK) {
            SessionManager session = new SessionManager(context);
            String atual = session.getToken();
            if (atual != null && !atual.equals(tokenUsadoNaChamada)) {
                return atual; // outra chamada já renovou enquanto esta esperava
            }
            String refresh = session.getRefreshToken();
            if (refresh == null || refresh.isEmpty()) {
                // Login feito antes do refresh token existir: só resolve logando de novo.
                if (session.estaLogado()) session.marcarSessaoEncerrada();
                return null;
            }
            try {
                Response<AutenticacaoResponseDto> r = RetrofitClient.getApi()
                        .refresh(new RefreshRequestDto(refresh)).execute();
                AutenticacaoResponseDto body = r.body();
                if (r.isSuccessful() && body != null && body.getToken() != null) {
                    session.salvarTokens(body.getToken(), body.getRefreshToken());
                    return body.getToken();
                }
                if (r.code() == 401) session.marcarSessaoEncerrada();
            } catch (IOException ignored) {
                // sem sinal: mantém os tokens e tenta de novo depois
            }
            return null;
        }
    }
}
