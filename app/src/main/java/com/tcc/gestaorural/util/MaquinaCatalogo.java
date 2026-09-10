package com.tcc.gestaorural.util;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Catálogo de máquinas (tipo → marca → modelos) carregado de
 * assets/maquinas_catalogo.json. Alimenta os dropdowns dependentes da tela Nova máquina.
 */
public final class MaquinaCatalogo {

    private static MaquinaCatalogo instancia;
    private JSONObject raiz;

    private MaquinaCatalogo(Context context) {
        try {
            InputStream is = context.getAssets().open("maquinas_catalogo.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String linha;
            while ((linha = br.readLine()) != null) sb.append(linha);
            br.close();
            raiz = new JSONObject(sb.toString());
        } catch (Exception e) {
            raiz = new JSONObject();
        }
    }

    public static MaquinaCatalogo get(Context context) {
        if (instancia == null) instancia = new MaquinaCatalogo(context.getApplicationContext());
        return instancia;
    }

    /** Tipos disponíveis (Trator, Colheitadeira, ...). */
    public List<String> tipos() {
        List<String> out = new ArrayList<>();
        for (Iterator<String> it = raiz.keys(); it.hasNext(); ) out.add(it.next());
        return out;
    }

    /** Marcas de um tipo. */
    public List<String> marcas(String tipo) {
        List<String> out = new ArrayList<>();
        JSONObject t = raiz.optJSONObject(tipo);
        if (t != null) for (Iterator<String> it = t.keys(); it.hasNext(); ) out.add(it.next());
        return out;
    }

    /** Modelos de uma marca dentro de um tipo. */
    public List<String> modelos(String tipo, String marca) {
        List<String> out = new ArrayList<>();
        JSONObject t = raiz.optJSONObject(tipo);
        if (t == null) return out;
        org.json.JSONArray arr = t.optJSONArray(marca);
        if (arr != null) for (int i = 0; i < arr.length(); i++) out.add(arr.optString(i));
        return out;
    }
}
