package com.tcc.gestaorural.ui.sobre;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.BaseActivity;

public class SobreActivity extends BaseActivity implements com.tcc.gestaorural.ui.equipe.EquipeSincronizador.TelaComum {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sobre);
        setTitle(R.string.sobre_title);

        View.OnClickListener emBreve = v ->
                Toast.makeText(this, R.string.msg_em_breve, Toast.LENGTH_SHORT).show();
        findViewById(R.id.rowFaq).setOnClickListener(emBreve);
        findViewById(R.id.rowSuporte).setOnClickListener(emBreve);
        findViewById(R.id.rowTermos).setOnClickListener(emBreve);
        findViewById(R.id.rowPrivacidade).setOnClickListener(emBreve);
    }
}
