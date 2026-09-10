package com.tcc.gestaorural.ui.metas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.util.SessionManager;

public class ListaMetasActivity extends BaseActivity {

    private MetaViewModel vm;
    private MetaAdapter adapter;
    private TextView txtVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_metas);
        setTitle(R.string.meta_lista_title);

        vm = new ViewModelProvider(this).get(MetaViewModel.class);
        txtVazio = findViewById(R.id.txtVazio);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MetaAdapter(mp -> {
            Intent i = new Intent(this, AcompanhamentoMetaActivity.class);
            i.putExtra(NavKeys.META_ID, mp.meta.metaId);
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabNova);
        fab.setOnClickListener(v -> startActivity(new Intent(this, NovaMetaActivity.class)));

        long usuarioId = new SessionManager(this).getUsuarioId();
        vm.iniciar(usuarioId);
        vm.getLista().observe(this, lista -> {
            adapter.submeter(lista);
            txtVazio.setVisibility((lista == null || lista.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }

    @Override protected boolean isTopLevel() { return true; }
    @Override protected int bottomNavItemId() { return R.id.nav_metas; }
}
