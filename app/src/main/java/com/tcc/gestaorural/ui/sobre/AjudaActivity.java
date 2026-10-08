package com.tcc.gestaorural.ui.sobre;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.equipe.EquipeSincronizador;

/**
 * Manual de ajuda (decidido em 13/08/2026): explica cada parte do app e os campos que mais
 * confundem. Um card por assunto, toque abre/fecha. O conteúdo fica em ConteudoAjuda.
 */
public class AjudaActivity extends BaseActivity implements EquipeSincronizador.TelaComum {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajuda);
        setTitle(R.string.ajuda_title);

        LinearLayout lista = findViewById(R.id.listaAjuda);
        for (String[] secao : ConteudoAjuda.SECOES) {
            View item = getLayoutInflater().inflate(R.layout.item_ajuda, lista, false);
            ((TextView) item.findViewById(R.id.txtTitulo)).setText(secao[0]);
            TextView texto = item.findViewById(R.id.txtTexto);
            texto.setText(secao[1]);
            View seta = item.findViewById(R.id.imgSeta);
            item.findViewById(R.id.cabecalho).setOnClickListener(v -> {
                boolean abrir = texto.getVisibility() != View.VISIBLE;
                texto.setVisibility(abrir ? View.VISIBLE : View.GONE);
                seta.setRotation(abrir ? 270 : 90);
            });
            lista.addView(item);
        }
    }
}
