package com.tcc.gestaorural.ui.metas;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.MetaFinanceira;
import com.tcc.gestaorural.data.local.model.SafraOpcao;
import com.tcc.gestaorural.data.repository.SafraRepository;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class NovaMetaActivity extends BaseActivity {

    private MetaViewModel vm;
    private TextInputEditText edtDescricao, edtValor, edtPrazo;
    private MaterialAutoCompleteTextView dropSafra;

    private final List<SafraOpcao> safras = new ArrayList<>();
    private long safraSelecionada = 0L;
    private long metaId = 0L;
    private MetaFinanceira existente;
    private long safraPendente = 0L;   // modo edição

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_meta);
        vm = new ViewModelProvider(this).get(MetaViewModel.class);
        long usuarioId = new SessionManager(this).getUsuarioId();

        edtDescricao = findViewById(R.id.edtDescricao);
        edtValor = findViewById(R.id.edtValor);
        edtPrazo = findViewById(R.id.edtPrazo);
        dropSafra = findViewById(R.id.dropSafra);
        Button btnSalvar = findViewById(R.id.btnSalvar);
        MaskUtils.aplicarData(edtPrazo);

        metaId = getIntent().getLongExtra(NavKeys.META_ID, 0L);
        setTitle(metaId == 0L ? R.string.meta_nova_title : R.string.meta_editar_title);

        new SafraRepository(this).observarOpcoes(usuarioId).observe(this, this::popularSafras);
        dropSafra.setOnItemClickListener((p, v, pos, id) -> {
            String sel = p.getItemAtPosition(pos).toString();
            for (SafraOpcao o : safras) if (o.rotulo().equals(sel)) { safraSelecionada = o.id; return; }
        });

        if (metaId != 0L) {
            vm.obter(metaId).observe(this, m -> {
                if (m != null && existente == null) {
                    existente = m;
                    safraPendente = m.safraId;
                    edtDescricao.setText(m.descricao);
                    edtValor.setText(String.valueOf(m.valorAlvo));
                    edtPrazo.setText(DateUtils.isoParaBr(m.prazo));
                    marcarSafra();
                }
            });
        }

        btnSalvar.setOnClickListener(v -> salvar());
        vm.getSalvoComId().observe(this, id -> {
            if (id == null) return;
            Toast.makeText(this, R.string.msg_meta_salva, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void popularSafras(List<SafraOpcao> lista) {
        safras.clear();
        if (lista != null) safras.addAll(lista);
        List<String> rotulos = new ArrayList<>();
        for (SafraOpcao o : safras) rotulos.add(o.rotulo());
        dropSafra.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rotulos));
        marcarSafra();
    }

    private void marcarSafra() {
        if (safraPendente == 0L) return;
        for (SafraOpcao o : safras) {
            if (o.id == safraPendente) {
                dropSafra.setText(o.rotulo(), false);
                safraSelecionada = o.id;
                return;
            }
        }
    }

    private void salvar() {
        if (safraSelecionada == 0L) { toast(R.string.err_meta_safra); return; }
        String desc = txt(edtDescricao);
        if (desc.isEmpty()) { toast(R.string.err_meta_descricao); return; }
        Double valor = MoneyUtils.parse(txt(edtValor));
        if (valor == null || valor <= 0) { toast(R.string.err_meta_valor); return; }

        MetaFinanceira m = (existente != null) ? existente : new MetaFinanceira();
        m.id = metaId;
        m.safraId = safraSelecionada;
        m.descricao = desc;
        m.valorAlvo = valor;
        m.prazo = DateUtils.brParaIso(txt(edtPrazo));
        vm.salvar(m);
    }

    private void toast(int res) { Toast.makeText(this, res, Toast.LENGTH_LONG).show(); }
    private String txt(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
}
