package com.tcc.gestaorural.data.remote;

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
 */
public final class RetrofitClient {

    private static final String BASE_URL = "https://api.safra-pay.com/";

    private static ApiService instance;

    private RetrofitClient() {}

    public static ApiService getApi() {
        if (instance == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instance = retrofit.create(ApiService.class);
        }
        return instance;
    }
}
