package com.tcc.gestaorural.data.remote;

import com.tcc.gestaorural.data.remote.dto.AlterarSenhaRequestDto;
import com.tcc.gestaorural.data.remote.dto.AuthResponseDto;
import com.tcc.gestaorural.data.remote.dto.EsqueciSenhaRequestDto;
import com.tcc.gestaorural.data.remote.dto.LoginRequestDto;
import com.tcc.gestaorural.data.remote.dto.MensagemResponseDto;
import com.tcc.gestaorural.data.remote.dto.PerfilRequestDto;
import com.tcc.gestaorural.data.remote.dto.PerfilResponseDto;
import com.tcc.gestaorural.data.remote.dto.PropriedadeSyncItemDto;
import com.tcc.gestaorural.data.remote.dto.RegisterRequestDto;
import com.tcc.gestaorural.data.remote.dto.SafraSyncItemDto;
import com.tcc.gestaorural.data.remote.dto.TalhaoSyncItemDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

/** Endpoints do backend SafraPay usados pelo app. */
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

    // Perfil (nome/cpf/telefone/nascimento) - e-mail e senha ficam de fora
    // (tem fluxos proprios). Rotas autenticadas, mesma logica do header acima.
    @GET("api/usuario/perfil")
    Call<PerfilResponseDto> buscarPerfil(@Header("Authorization") String bearerToken);

    @PUT("api/usuario/perfil")
    Call<PerfilResponseDto> atualizarPerfil(@Header("Authorization") String bearerToken,
                                             @Body PerfilRequestDto request);

    // --- Sincronizacao offline-first (Fase 1: Propriedade + Talhao) ---
    // Push manda o que foi criado/editado offline (sincronizado = 0); o servidor
    // devolve o estado final de cada item (aceito ou nao, "last write wins").
    // Pull traz o que mudou no servidor desde a ultima sincronizacao ("desde"
    // e um instante ISO-8601; null/vazio = primeira sincronizacao, traz tudo).

    @POST("api/propriedades/sync")
    Call<List<PropriedadeSyncItemDto>> sincronizarPushPropriedades(
            @Header("Authorization") String bearerToken, @Body List<PropriedadeSyncItemDto> itens);

    @GET("api/propriedades/sync")
    Call<List<PropriedadeSyncItemDto>> sincronizarPullPropriedades(
            @Header("Authorization") String bearerToken, @Query("desde") String desde);

    @POST("api/talhoes/sync")
    Call<List<TalhaoSyncItemDto>> sincronizarPushTalhoes(
            @Header("Authorization") String bearerToken, @Body List<TalhaoSyncItemDto> itens);

    @GET("api/talhoes/sync")
    Call<List<TalhaoSyncItemDto>> sincronizarPullTalhoes(
            @Header("Authorization") String bearerToken, @Query("desde") String desde);

    @POST("api/safras/sync")
    Call<List<SafraSyncItemDto>> sincronizarPushSafras(
            @Header("Authorization") String bearerToken, @Body List<SafraSyncItemDto> itens);

    @GET("api/safras/sync")
    Call<List<SafraSyncItemDto>> sincronizarPullSafras(
            @Header("Authorization") String bearerToken, @Query("desde") String desde);
}
