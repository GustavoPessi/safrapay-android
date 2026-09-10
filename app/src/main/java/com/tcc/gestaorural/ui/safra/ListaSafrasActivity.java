package com.tcc.gestaorural.ui.safra;

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

public class ListaSafrasActivity extends BaseActivity {

    private SafraViewModel vm;
    private SafraAdapter adapter;
    private TextView txtVazio;
    private long talhaoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_safras);
        setTitle(R.string.safra_lista_title);

        vm = new ViewModelProvider(this).get(SafraViewModel.class);
        talhaoId = getIntent().getLongExtra(NavKeys.TALHAO_ID, 0L);
        txtVazio = findViewById(R.id.txtVazio);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SafraAdapter(s -> {
            Intent i = new Intent(this, DetalheSafraActivity.class);
            i.putExtra(NavKeys.SAFRA_ID, s.id);
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabNova);
        fab.setOnClickListener(v -> {
            Intent i = new Intent(this, NovaSafraActivity.class);
            i.putExtra(NavKeys.TALHAO_ID, talhaoId);
            startActivity(i);
        });

        vm.listar(talhaoId).observe(this, lista -> {
            adapter.submeter(lista);
            txtVazio.setVisibility((lista == null || lista.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }
}
