package com.tcc.gestaorural.ui.talhao;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.NavKeys;

public class ListaTalhoesActivity extends BaseActivity {

    private TalhaoViewModel vm;
    private TalhaoAdapter adapter;
    private TextView txtVazio;
    private long propriedadeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_talhoes);
        setTitle(R.string.talhao_lista_title);

        vm = new ViewModelProvider(this).get(TalhaoViewModel.class);
        propriedadeId = getIntent().getLongExtra(NavKeys.PROPRIEDADE_ID, 0L);
        txtVazio = findViewById(R.id.txtVazio);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TalhaoAdapter(t -> {
            Intent i = new Intent(this, DetalheTalhaoActivity.class);
            i.putExtra(NavKeys.TALHAO_ID, t.id);
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabNova);
        fab.setOnClickListener(v -> {
            Intent i = new Intent(this, NovoTalhaoActivity.class);
            i.putExtra(NavKeys.PROPRIEDADE_ID, propriedadeId);
            startActivity(i);
        });

        vm.listar(propriedadeId).observe(this, lista -> {
            adapter.submeter(lista);
            txtVazio.setVisibility((lista == null || lista.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }
}
