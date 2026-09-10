package com.tcc.gestaorural.ui.cultura;

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

public class ListaCulturasActivity extends BaseActivity {
    private CulturaViewModel vm;
    private CulturaAdapter adapter;
    private TextView txtVazio;
    private long propriedadeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_culturas);
        setTitle(R.string.cultura_lista_title);
        vm = new ViewModelProvider(this).get(CulturaViewModel.class);
        propriedadeId = getIntent().getLongExtra(NavKeys.PROPRIEDADE_ID, 0L);
        txtVazio = findViewById(R.id.txtVazio);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CulturaAdapter(c -> {
            Intent i = new Intent(this, NovaCulturaActivity.class);
            i.putExtra(NavKeys.PROPRIEDADE_ID, propriedadeId);
            i.putExtra(NavKeys.CULTURA_ID, c.id);
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabNova);
        fab.setOnClickListener(v -> {
            Intent i = new Intent(this, NovaCulturaActivity.class);
            i.putExtra(NavKeys.PROPRIEDADE_ID, propriedadeId);
            startActivity(i);
        });

        vm.listarPorPropriedade(propriedadeId).observe(this, lista -> {
            adapter.submeter(lista);
            txtVazio.setVisibility((lista == null || lista.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }
}
