package com.tcc.gestaorural.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

/**
 * Geolocalização Nível 1 (ponto/pin) — captura a coordenada GPS atual do aparelho
 * para preencher latitude/longitude de Propriedade ou Talhão. Não depende de SDK
 * de mapa, só de play-services-location.
 *
 * Uso: a Activity registra o ActivityResultLauncher<String> de permissão no
 * onCreate (obrigatório, tem que ser antes do STARTED) chamando {@link #obter}
 * dentro do callback quando concedida; o botão "Usar minha localização" chama
 * {@link #solicitar}, que já captura direto se a permissão já estiver concedida.
 */
public class LocationHelper {

    public interface Callback {
        void onLocalizacaoObtida(double latitude, double longitude);
        void onPermissaoNegada();
        void onIndisponivel();
    }

    public static boolean temPermissao(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Chamar no clique do botão "Usar minha localização". */
    public static void solicitar(Activity activity, ActivityResultLauncher<String> permissionLauncher, Callback callback) {
        if (temPermissao(activity)) obter(activity, callback);
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /** Chamar dentro do callback do ActivityResultLauncher quando a permissão for concedida. */
    public static void obter(Activity activity, Callback callback) {
        if (!temPermissao(activity)) { callback.onPermissaoNegada(); return; }
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(activity);
        CancellationTokenSource cts = new CancellationTokenSource();
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) callback.onLocalizacaoObtida(location.getLatitude(), location.getLongitude());
                    else callback.onIndisponivel();
                })
                .addOnFailureListener(e -> callback.onIndisponivel());
    }
}
