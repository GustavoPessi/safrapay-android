package com.tcc.gestaorural.ui;

import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.dashboard.DashboardActivity;
import com.tcc.gestaorural.ui.financeiro.FinanceiroActivity;
import com.tcc.gestaorural.ui.metas.ListaMetasActivity;
import com.tcc.gestaorural.ui.perfil.PerfilActivity;

/**
 * Activity base do SafraPay: injeta a barra superior verde (título + voltar) e,
 * quando a tela é uma aba principal, a navegação inferior do Design System.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected MaterialToolbar toolbar;

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        View base = getLayoutInflater().inflate(R.layout.base_activity, null);
        FrameLayout container = base.findViewById(R.id.content_container);
        getLayoutInflater().inflate(layoutResID, container, true);
        super.setContentView(base);

        toolbar = base.findViewById(R.id.topbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(!isTopLevel());
        }

        configurarBottomNav(base.findViewById(R.id.bottom_nav));
    }

    private void configurarBottomNav(BottomNavigationView nav) {
        int selecionado = bottomNavItemId();
        if (selecionado == 0) { nav.setVisibility(View.GONE); return; }
        nav.setVisibility(View.VISIBLE);
        nav.setSelectedItemId(selecionado);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == bottomNavItemId()) return true;
            Intent destino = destinoAba(id);
            if (destino != null) {
                destino.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(destino);
                overridePendingTransition(0, 0);
            }
            return true;
        });
    }

    private Intent destinoAba(int id) {
        if (id == R.id.nav_inicio) return new Intent(this, DashboardActivity.class);
        if (id == R.id.nav_financas) {
            Intent i = new Intent(this, FinanceiroActivity.class);
            i.putExtra(NavKeys.FINANCAS_GLOBAL, true);
            return i;
        }
        if (id == R.id.nav_metas) return new Intent(this, ListaMetasActivity.class);
        if (id == R.id.nav_perfil) return new Intent(this, PerfilActivity.class);
        return null;
    }

    /** Telas raiz (sem seta de voltar) devem sobrescrever retornando true. */
    protected boolean isTopLevel() { return false; }

    /** Abas principais devem retornar o id do item selecionado (0 = sem bottom nav). */
    protected int bottomNavItemId() { return 0; }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
