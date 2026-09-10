package com.tcc.gestaorural.ui.metas;

import android.os.Bundle;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.BaseActivity;

/** Aba Metas (placeholder). O módulo completo de metas entra na próxima etapa. */
public class MetasPlaceholderActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metas_placeholder);
        setTitle(R.string.metas_title);
    }

    @Override protected boolean isTopLevel() { return true; }
    @Override protected int bottomNavItemId() { return R.id.nav_metas; }
}
