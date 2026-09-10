package com.tcc.gestaorural.ui.safra;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.LiveData;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.MetaFinanceira;
import com.tcc.gestaorural.data.local.entity.Propriedade;
import com.tcc.gestaorural.data.local.entity.Safra;
import com.tcc.gestaorural.data.local.entity.Talhao;
import com.tcc.gestaorural.data.repository.MetaRepository;
import com.tcc.gestaorural.data.repository.PropriedadeRepository;
import com.tcc.gestaorural.data.repository.SafraRepository;
import com.tcc.gestaorural.data.repository.TalhaoRepository;
import com.tcc.gestaorural.ui.BaseActivity;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.sucesso.SucessoActivity;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluxo guiado de uma tela só: Propriedade -> Talhão (existente ou novo, criado na hora)
 * -> dados da safra -> meta financeira opcional -> salvar tudo de uma vez.
 * Não usa ViewModel: como coordena 4 entidades em sequência, é mais direto encadear
 * os callbacks dos repositórios (mesmo padrão de acesso direto a repositório já usado
 * em NovaDespesaActivity para a lista de máquinas).
 */
public class NovaSafraRapidaActivity extends BaseActivity {

    private PropriedadeRepository propriedadeRepo;
    private TalhaoRepository talhaoRepo;
    private SafraRepository safraRepo;
    private MetaRepository metaRepo;

    private MaterialAutoCompleteTextView dropPropriedade, dropTalhao;
    private android.view.View layoutNovoTalhao, layoutMeta;
    private TextInputEditText edtNovoTalhaoNome, edtNovoTalhaoCultura, edtNovoTalhaoArea;
    private TextInputEditText edtAno, edtVariedade, edtInicio, edtFim;
    private MaterialSwitch swMeta;
    private TextInputEditText edtMetaDescricao, edtMetaValor, edtMetaPrazo;
    private Button btnSalvar;

    private final List<Propriedade> propriedades = new ArrayList<>();
    private final List<Talhao> talhoes = new ArrayList<>();
    private LiveData<List<Talhao>> talhoesLiveData;

    private long propriedadeSelecionada = 0L;
    private long talhaoSelecionado = 0L;
    private boolean modoNovoTalhao = false;
    private String opcaoNovoTalhao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_safra_rapida);
        setTitle(R.string.safra_rapida_title);

        long usuarioId = new SessionManager(this).getUsuarioId();
        propriedadeRepo = new PropriedadeRepository(this);
        talhaoRepo = new TalhaoRepository(this);
        safraRepo = new SafraRepository(this);
        metaRepo = new MetaRepository(this);
        opcaoNovoTalhao = getString(R.string.opcao_novo_talhao);

        dropPropriedade = findViewById(R.id.dropPropriedade);
        dropTalhao = findViewById(R.id.dropTalhao);
        layoutNovoTalhao = findViewById(R.id.layoutNovoTalhao);
        edtNovoTalhaoNome = findViewById(R.id.edtNovoTalhaoNome);
        edtNovoTalhaoCultura = findViewById(R.id.edtNovoTalhaoCultura);
        edtNovoTalhaoArea = findViewById(R.id.edtNovoTalhaoArea);
        edtAno = findViewById(R.id.edtAno);
        edtVariedade = findViewById(R.id.edtVariedade);
        edtInicio = findViewById(R.id.edtInicio);
        edtFim = findViewById(R.id.edtFim);
        swMeta = findViewById(R.id.swMeta);
        layoutMeta = findViewById(R.id.layoutMeta);
        edtMetaDescricao = findViewById(R.id.edtMetaDescricao);
        edtMetaValor = findViewById(R.id.edtMetaValor);
        edtMetaPrazo = findViewById(R.id.edtMetaPrazo);
        btnSalvar = findViewById(R.id.btnSalvar);

        MaskUtils.aplicarData(edtInicio);
        MaskUtils.aplicarData(edtFim);
        MaskUtils.aplicarData(edtMetaPrazo);

        propriedadeRepo.listar(usuarioId).observe(this, this::popularPropriedades);

        dropPropriedade.setOnItemClickListener((parent, view, position, id) -> {
            String sel = parent.getItemAtPosition(position).toString();
            for (Propriedade p : propriedades) {
                if (p.nome.equals(sel)) {
                    propriedadeSelecionada = p.id;
                    dropTalhao.setText("", false);
                    talhaoSelecionado = 0L;
                    ativarModoNovoTalhao(false);
                    carregarTalhoes(p.id);
                    return;
                }
            }
        });

        dropTalhao.setOnItemClickListener((parent, view, position, id) -> {
            String sel = parent.getItemAtPosition(position).toString();
            if (sel.equals(opcaoNovoTalhao)) {
                talhaoSelecionado = 0L;
                ativarModoNovoTalhao(true);
                return;
            }
            ativarModoNovoTalhao(false);
            for (Talhao t : talhoes) {
                if (t.nome.equals(sel)) { talhaoSelecionado = t.id; return; }
            }
            talhaoSelecionado = 0L;
        });

        swMeta.setOnCheckedChangeListener((btn, checked) ->
                layoutMeta.setVisibility(checked ? android.view.View.VISIBLE : android.view.View.GONE));

        btnSalvar.setOnClickListener(v -> salvar());
    }

    private void popularPropriedades(List<Propriedade> lista) {
        propriedades.clear();
        if (lista != null) propriedades.addAll(lista);
        List<String> rotulos = new ArrayList<>();
        for (Propriedade p : propriedades) rotulos.add(p.nome);
        dropPropriedade.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rotulos));
    }

    private void carregarTalhoes(long propriedadeId) {
        if (talhoesLiveData != null) talhoesLiveData.removeObservers(this);
        talhoesLiveData = talhaoRepo.listar(propriedadeId);
        talhoesLiveData.observe(this, this::popularTalhoes);
    }

    private void popularTalhoes(List<Talhao> lista) {
        talhoes.clear();
        if (lista != null) talhoes.addAll(lista);
        List<String> rotulos = new ArrayList<>();
        for (Talhao t : talhoes) rotulos.add(t.nome);
        rotulos.add(opcaoNovoTalhao);
        dropTalhao.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rotulos));
    }

    private void ativarModoNovoTalhao(boolean ativo) {
        modoNovoTalhao = ativo;
        layoutNovoTalhao.setVisibility(ativo ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void salvar() {
        if (propriedadeSelecionada == 0L) { toast(R.string.err_safra_rapida_propriedade); return; }

        String novoTalhaoNome = txt(edtNovoTalhaoNome);
        if (modoNovoTalhao && novoTalhaoNome.isEmpty()) { toast(R.string.err_talhao_nome_obrigatorio); return; }
        if (!modoNovoTalhao && talhaoSelecionado == 0L) { toast(R.string.err_safra_rapida_talhao); return; }

        String ano = txt(edtAno);
        if (ano.isEmpty()) { toast(R.string.err_safra_obrigatorio); return; }

        boolean definirMeta = swMeta.isChecked();
        String metaDescricao = txt(edtMetaDescricao);
        Double metaValor = definirMeta ? MoneyUtils.parse(txt(edtMetaValor)) : null;
        if (definirMeta) {
            if (metaDescricao.isEmpty()) { toast(R.string.err_meta_descricao); return; }
            if (metaValor == null || metaValor <= 0) { toast(R.string.err_meta_valor); return; }
        }

        btnSalvar.setEnabled(false);

        if (modoNovoTalhao) {
            Talhao t = new Talhao();
            t.propriedadeId = propriedadeSelecionada;
            t.nome = novoTalhaoNome;
            String cultura = txt(edtNovoTalhaoCultura);
            t.cultura = cultura.isEmpty() ? null : cultura;
            String area = txt(edtNovoTalhaoArea);
            t.areaHa = area.isEmpty() ? null : safeDouble(area);
            talhaoRepo.salvar(t, novoTalhaoId ->
                    criarSafra(novoTalhaoId, ano, definirMeta, metaDescricao, metaValor));
        } else {
            criarSafra(talhaoSelecionado, ano, definirMeta, metaDescricao, metaValor);
        }
    }

    /** Ano/variedade/datas da safra seguem o mesmo formato salvo por NovaSafraActivity (texto BR, sem converter). */
    private void criarSafra(long talhaoId, String ano, boolean definirMeta, String metaDescricao, Double metaValor) {
        Safra s = new Safra();
        s.talhaoId = talhaoId;
        s.anoSafra = ano;
        s.variedade = nullSeVazio(txt(edtVariedade));
        s.dataInicio = nullSeVazio(txt(edtInicio));
        s.dataFim = nullSeVazio(txt(edtFim));
        safraRepo.salvar(s, novaSafraId -> {
            if (definirMeta) criarMeta(novaSafraId, metaDescricao, metaValor);
            else finalizarComSucesso();
        });
    }

    private void criarMeta(long safraId, String descricao, double valor) {
        MetaFinanceira m = new MetaFinanceira();
        m.safraId = safraId;
        m.descricao = descricao;
        m.valorAlvo = valor;
        m.prazo = com.tcc.gestaorural.util.DateUtils.brParaIso(txt(edtMetaPrazo));
        metaRepo.salvar(m, id -> finalizarComSucesso());
    }

    private void finalizarComSucesso() {
        runOnUiThread(() -> {
            Intent i = new Intent(this, SucessoActivity.class);
            i.putExtra(NavKeys.SUCESSO_MSG, getString(R.string.msg_safra_rapida_salva));
            startActivity(i);
            finish();
        });
    }

    private void toast(int res) { Toast.makeText(this, res, Toast.LENGTH_LONG).show(); }
    private String nullSeVazio(String s) { return TextUtils.isEmpty(s) ? null : s; }
    private String txt(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }
    private Double safeDouble(String s) {
        try { return Double.parseDouble(s.replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }
}
