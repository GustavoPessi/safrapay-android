package com.tcc.gestaorural.ui.comprovante;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.AppDatabase;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.util.ComprovanteUtils;

/** Comprovante em tela cheia, com botão de compartilhar. */
public class ComprovanteActivity extends BaseActivity {

    private static final String EXTRA_CAMINHO = "extra_comprovante_caminho";
    private static final String EXTRA_LEGENDA = "extra_comprovante_legenda";

    public static void abrir(Activity origem, String caminho, String legenda) {
        Intent i = new Intent(origem, ComprovanteActivity.class);
        i.putExtra(EXTRA_CAMINHO, caminho);
        i.putExtra(EXTRA_LEGENDA, legenda);
        origem.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comprovante);
        setTitle(R.string.comprovante_tela_titulo);

        final String caminho = getIntent().getStringExtra(EXTRA_CAMINHO);
        final String legenda = getIntent().getStringExtra(EXTRA_LEGENDA);

        ImageView img = findViewById(R.id.imgComprovante);
        TextView txt = findViewById(R.id.txtLegenda);
        txt.setText(legenda);
        txt.setVisibility(legenda == null || legenda.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnCompartilhar).setOnClickListener(v ->
                ComprovanteUtils.compartilhar(this, caminho, legenda));

        AppDatabase.IO.execute(() -> {
            Bitmap b = ComprovanteUtils.miniatura(caminho, 2048);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                if (b != null) img.setImageBitmap(b);
                else txt.setText(R.string.chat_comprovante_sem_foto);
            });
        });
    }
}
