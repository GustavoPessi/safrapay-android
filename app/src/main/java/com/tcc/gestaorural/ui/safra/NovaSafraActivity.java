package com.tcc.gestaorural.ui.safra;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.data.repository.TalhaoRepository;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.util.MaskUtils;

public class NovaSafraActivity extends BaseActivity {

    private SafraViewModel vm;
    private TextView txtTalhaoInfo;
    private TextInputEditText edtVariedade, edtAno, edtInicio, edtFim;
    private long safraId = 0L, talhaoId = 0L;
    private Safra existente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_safra);
        vm = new ViewModelProvider(this).get(SafraViewModel.class);

        txtTalhaoInfo = findViewById(R.id.txtTalhaoInfo);
        edtVariedade = findViewById(R.id.edtVariedade);
        edtAno = findViewById(R.id.edtAno);
        edtInicio = findViewById(R.id.edtInicio);
        edtFim = findViewById(R.id.edtFim);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        MaskUtils.aplicarData(edtInicio);
        MaskUtils.aplicarData(edtFim);

        talhaoId = getIntent().getLongExtra(NavKeys.TALHAO_ID, 0L);
        safraId = getIntent().getLongExtra(NavKeys.SAFRA_ID, 0L);
        setTitle(safraId == 0L ? R.string.safra_nova_title : R.string.safra_editar_title);

        if (safraId != 0L) {
            vm.obterResumo(safraId).observe(this, r -> {
                if (r != null && existente == null) {
                    existente = new Safra();
                    existente.id = r.id;
                    existente.talhaoId = r.talhaoId;
                    talhaoId = r.talhaoId;
                    edtVariedade.setText(r.variedade);
                    edtAno.setText(r.anoSafra);
                    edtInicio.setText(r.dataInicio);
                    edtFim.setText(r.dataFim);
                    mostrarTalhao(talhaoId);
                }
            });
        } else {
            mostrarTalhao(talhaoId);
        }

        btnSalvar.setOnClickListener(v -> salvar());
        vm.getSalvoComId().observe(this, id -> {
            if (id == null) return;
            Toast.makeText(this, R.string.msg_safra_salva, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    /** Mostra o talhão e a sua cultura (a cultura da safra vem daqui). */
    private void mostrarTalhao(long talhaoId) {
        if (talhaoId == 0L) return;
        new TalhaoRepository(this).obter(talhaoId).observe(this, t -> {
            if (t == null) return;
            String cultura = (t.cultura == null || t.cultura.trim().isEmpty())
                    ? getString(R.string.safra_sem_cultura) : t.cultura;
            txtTalhaoInfo.setText(getString(R.string.safra_talhao_fmt, t.nome, cultura));
        });
    }

    private void salvar() {
        String ano = txt(edtAno);
        if (ano.isEmpty()) {
            Toast.makeText(this, R.string.err_safra_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }
        if (talhaoId == 0L) {
            Toast.makeText(this, R.string.msg_em_breve, Toast.LENGTH_SHORT).show();
            return;
        }
        Safra s = (existente != null) ? existente : new Safra();
        s.id = safraId;
        s.talhaoId = talhaoId;
        s.anoSafra = ano;
        s.variedade = nullSeVazio(txt(edtVariedade));
        s.dataInicio = nullSeVazio(txt(edtInicio));
        s.dataFim = nullSeVazio(txt(edtFim));
        vm.salvar(s);
    }

    private String nullSeVazio(String s) { return TextUtils.isEmpty(s) ? null : s; }
    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
