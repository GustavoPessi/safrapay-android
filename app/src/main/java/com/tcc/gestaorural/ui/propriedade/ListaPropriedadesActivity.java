package com.tcc.gestaorural.ui.propriedade;

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
import com.tcc.gestaorural.util.SessionManager;

public class ListaPropriedadesActivity extends BaseActivity {

    private PropriedadeViewModel vm;
    private PropriedadeAdapter adapter;
    private TextView txtVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_propriedades);
        setTitle(R.string.prop_lista_title);

        vm = new ViewModelProvider(this).get(PropriedadeViewModel.class);
        txtVazio = findViewById(R.id.txtVazio);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PropriedadeAdapter(p -> {
            Intent i = new Intent(this, DetalhePropriedadeActivity.class);
            i.putExtra(Extras.PROPRIEDADE_ID, p.id);
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabNova);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, NovaPropriedadeActivity.class)));

        long usuarioId = new SessionManager(this).getUsuarioId();
        vm.listar(usuarioId).observe(this, lista -> {
            adapter.submeter(lista);
            txtVazio.setVisibility((lista == null || lista.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }
}
