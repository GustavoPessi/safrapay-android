package com.tcc.gestaorural.data.remote;

import com.tcc.gestaorural.data.remote.dto.AlterarSenhaRequestDto;
import com.tcc.gestaorural.data.remote.dto.AuthResponseDto;
import com.tcc.gestaorural.data.remote.dto.EsqueciSenhaRequestDto;
import com.tcc.gestaorural.data.remote.dto.LoginRequestDto;
import com.tcc.gestaorural.data.remote.dto.MensagemResponseDto;
import com.tcc.gestaorural.data.remote.dto.RegisterRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/** Endpoints do backend SafraPay usados pelo app (fase de testes: so auth por enquanto). */
public interface ApiService {

    @POST("api/auth/registrar")
    Call<MensagemResponseDto> registrar(@Body RegisterRequestDto request);

    @POST("api/auth/login")
    Call<AuthResponseDto> login(@Body LoginRequestDto request);

    // Backend sempre responde com sucesso aqui (mesma mensagem exista ou nao o
    // e-mail, pra nao vazar quais contas existem). Se existir, manda um e-mail
    // com link pra pagina de redefinir senha (redefinir-senha.html no backend).
    @POST("api/auth/esqueci-senha")
    Call<MensagemResponseDto> esqueciSenha(@Body EsqueciSenhaRequestDto request);

    // Rota autenticada: precisa do header "Authorization: Bearer <token>"
    // (o app ainda nao tem um interceptor global de auth, entao passamos aqui).
    @PUT("api/auth/alterar-senha")
    Call<MensagemResponseDto> alterarSenha(@Header("Authorization") String bearerToken,
                                            @Body AlterarSenhaRequestDto request);
}
