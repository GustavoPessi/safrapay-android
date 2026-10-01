package com.tcc.gestaorural.data.remote;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente Retrofit pro backend SafraPay.
 *
 * Aponta pro dominio proprio via HTTPS (Nginx + certificado Let's Encrypt na
 * EC2, configurado em 28/09/2026). O Nginx escuta na 443 padrao e repassa pro
 * Spring Boot na 8080 interna - por isso a URL nao tem porta. Antes disso o app
 * falava direto com o IP da EC2 em HTTP puro (http://3.140.103.43:8080/).
 *
 * 01/10/2026: interceptor de refresh token (renova o JWT de 24h sozinho) e
 * timeouts maiores - foto e planilha no assistente de IA podem levar ~1 min.
 */
public final class RetrofitClient {

    private static final String BASE_URL = "https://api.safra-pay.com/";

    private static ApiService instance;
    private static Context appContext;

    private RetrofitClient() {}

    /** Chamado no GestaoRuralApp.onCreate, antes de qualquer chamada. */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static synchronized ApiService getApi() {
        if (instance == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS);
            if (appContext != null) builder.addInterceptor(new TokenRefreshInterceptor(appContext));
            builder.addInterceptor(logging);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instance = retrofit.create(ApiService.class);
        }
        return instance;
    }
}
