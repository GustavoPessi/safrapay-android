package com.tcc.gestaorural.data.remote;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.tcc.gestaorural.data.remote.dto.AnotacaoMaquinaSyncItemDto;

import java.io.IOException;
import java.util.Map;

/**
 * O Gson não manda campos null — some do JSON. No sync de anotação o servidor trata
 * "campo ausente" como "manter o valor atual" e "null" como "limpar" (contrato do
 * multiusuário, seção 6). Então, pra conseguir TIRAR o prazo ou o responsável de uma tarefa,
 * "prazo" e "responsavelUuid" precisam ir como null explícito. Esta fábrica faz isso só para
 * esses dois campos do AnotacaoMaquinaSyncItemDto; o resto do JSON segue igual.
 */
public class NulosExplicitosFactory implements TypeAdapterFactory {

    private static final String[] CAMPOS = { "prazo", "responsavelUuid" };

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() != AnotacaoMaquinaSyncItemDto.class) return null;
        final TypeAdapter<T> padrao = gson.getDelegateAdapter(this, type);
        final TypeAdapter<JsonElement> elemento = gson.getAdapter(JsonElement.class);
        return new TypeAdapter<T>() {
            @Override public void write(JsonWriter out, T valor) throws IOException {
                if (valor == null) { out.nullValue(); return; }
                JsonObject obj = padrao.toJsonTree(valor).getAsJsonObject();
                out.beginObject();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    out.name(e.getKey());
                    elemento.write(out, e.getValue());
                }
                boolean antes = out.getSerializeNulls();
                out.setSerializeNulls(true);
                for (String campo : CAMPOS) if (!obj.has(campo)) out.name(campo).nullValue();
                out.setSerializeNulls(antes);
                out.endObject();
            }
            @Override public T read(JsonReader in) throws IOException { return padrao.read(in); }
        };
    }
}
