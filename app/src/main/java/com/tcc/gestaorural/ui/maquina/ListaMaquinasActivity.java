package com.tcc.gestaorural.ui.maquina;

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

public class ListaMaquinasActivity extends BaseActivity {

    private MaquinaViewModel vm;
    private MaquinaAdapter adapter;
    private TextView txtVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_maquinas);
        setTitle(R.string.maquina_lista_title);

        vm = new ViewModelProvider(this).get(MaquinaViewModel.class);
        txtVazio = findViewById(R.id.txtVazio);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MaquinaAdapter(m -> {
            Intent i = new Intent(this, DetalheMaquinaActivity.class);
            i.putExtra(NavKeys.MAQUINA_ID, m.id);
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabNova);
        fab.setOnClickListener(v -> startActivity(new Intent(this, NovaMaquinaActivity.class)));

        long usuarioId = new SessionManager(this).getUsuarioId();
        vm.listarComGasto(usuarioId).observe(this, lista -> {
            adapter.submeter(lista);
            txtVazio.setVisibility((lista == null || lista.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }
}
