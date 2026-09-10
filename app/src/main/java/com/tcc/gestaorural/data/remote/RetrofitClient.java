package com.tcc.gestaorural.data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente Retrofit pro backend SafraPay.
 *
 * FASE DE TESTES: aponta direto pro IP publico da instancia EC2, via HTTP puro
 * (sem HTTPS ainda). Trocar por um dominio proprio com certificado quando sair
 * da fase de testes - ver pendencia de dominio anotada no Notion do projeto.
 */
public final class RetrofitClient {

    private static final String BASE_URL = "http://3.140.103.43:8080/";

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
