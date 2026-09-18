package com.tcc.gestaorural.ui.talhao;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tcc.gestaorural.ui.BaseActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Talhao;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.util.LocationHelper;

public class NovoTalhaoActivity extends BaseActivity {

    private TalhaoViewModel vm;
    private TextInputEditText edtNome, edtCultura, edtArea;
    private TextView tvLocalizacaoStatus;
    private long talhaoId = 0L, propriedadeId = 0L;
    private Talhao existente;
    private Double latitude, longitude;   // capturadas pelo GPS (Geolocalização Nível 1)

    private final LocationHelper.Callback locationCallback = new LocationHelper.Callback() {
        @Override public void onLocalizacaoObtida(double lat, double lng) {
            latitude = lat; longitude = lng;
            atualizarStatusLocalizacao();
            Toast.makeText(NovoTalhaoActivity.this, R.string.localizacao_capturada, Toast.LENGTH_SHORT).show();
        }
        @Override public void onPermissaoNegada() {
            Toast.makeText(NovoTalhaoActivity.this, R.string.localizacao_permissao_negada, Toast.LENGTH_LONG).show();
        }
        @Override public void onIndisponivel() {
            Toast.makeText(NovoTalhaoActivity.this, R.string.localizacao_indisponivel, Toast.LENGTH_LONG).show();
        }
    };

    private final ActivityResultLauncher<String> permissaoLocalizacao = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedida -> {
                if (concedida) LocationHelper.obter(this, locationCallback);
                else locationCallback.onPermissaoNegada();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novo_talhao);
        vm = new ViewModelProvider(this).get(TalhaoViewModel.class);

        edtNome = findViewById(R.id.edtNome);
        edtCultura = findViewById(R.id.edtCultura);
        edtArea = findViewById(R.id.edtArea);
        tvLocalizacaoStatus = findViewById(R.id.tvLocalizacaoStatus);
        Button btnSalvar = findViewById(R.id.btnSalvar);
        Button btnUsarLocalizacao = findViewById(R.id.btnUsarLocalizacao);

        propriedadeId = getIntent().getLongExtra(NavKeys.PROPRIEDADE_ID, 0L);
        talhaoId = getIntent().getLongExtra(NavKeys.TALHAO_ID, 0L);
        setTitle(talhaoId == 0L ? R.string.talhao_novo_title : R.string.talhao_editar_title);

        if (talhaoId != 0L) {
            vm.obter(talhaoId).observe(this, t -> {
                if (t != null && existente == null) {
                    existente = t;
                    propriedadeId = t.propriedadeId;
                    edtNome.setText(t.nome);
                    edtCultura.setText(t.cultura);
                    edtArea.setText(t.areaHa == null ? "" : String.valueOf(t.areaHa));
                    latitude = t.latitude;
                    longitude = t.longitude;
                    atualizarStatusLocalizacao();
                }
            });
        } else {
            atualizarStatusLocalizacao();
        }

        btnUsarLocalizacao.setOnClickListener(v -> LocationHelper.solicitar(this, permissaoLocalizacao, locationCallback));
        btnSalvar.setOnClickListener(v -> salvar());
        vm.getSalvoComId().observe(this, id -> {
            if (id == null) return;
            Toast.makeText(this, R.string.msg_talhao_salvo, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void atualizarStatusLocalizacao() {
        if (latitude != null && longitude != null) {
            tvLocalizacaoStatus.setText(getString(R.string.localizacao_salva_formato, latitude, longitude));
        } else {
            tvLocalizacaoStatus.setText(R.string.localizacao_nao_definida);
        }
    }

    private void salvar() {
        String nome = txt(edtNome);
        if (nome.isEmpty()) {
            Toast.makeText(this, R.string.err_talhao_nome_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }
        Talhao t = (existente != null) ? existente : new Talhao();
        t.id = talhaoId;
        t.propriedadeId = propriedadeId;
        t.nome = nome;
        String cultura = txt(edtCultura);
        t.cultura = TextUtils.isEmpty(cultura) ? null : cultura;
        String area = txt(edtArea);
        t.areaHa = TextUtils.isEmpty(area) ? null : safeDouble(area);
        t.latitude = latitude;
        t.longitude = longitude;
        vm.salvar(t);
    }

    private Double safeDouble(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
