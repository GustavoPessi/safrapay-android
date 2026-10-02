package com.tcc.gestaorural.ui.financeiro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.tcc.gestaorural.ui.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.CategoriaDespesa;
import com.tcc.gestaorural.data.local.entity.Despesa;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.data.repository.MaquinaRepository;
import com.tcc.gestaorural.data.local.model.SafraOpcao;
import com.tcc.gestaorural.data.local.entity.Anexo;
import com.tcc.gestaorural.ui.NavKeys;
import com.tcc.gestaorural.ui.comprovante.ComprovanteCampo;
import com.tcc.gestaorural.ui.sucesso.SucessoActivity;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.MoneyUtils;
import com.tcc.gestaorural.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class NovaDespesaActivity extends BaseActivity {

    /** Nome exato da categoria que libera o seletor de máquina (ver AppDatabase.CATEGORIAS_SEED). */
    private static final String CATEGORIA_MAQUINAS = "Máquinas";

    private DespesaViewModel vm;
    private TextInputEditText edtDescricao, edtValor, edtData;
    private MaterialAutoCompleteTextView dropSafra;
    private ChipGroup chipsCategoria;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView dropMaquina;
    private View tilMaquina;
    private final java.util.List<Maquina> maquinas = new java.util.ArrayList<>();
    private final List<CategoriaDespesa> categoriasCarregadas = new ArrayList<>();
    private Long maquinaSelecionada = null;
    private Long maquinaPendente = null;

    private final List<SafraOpcao> opcoes = new ArrayList<>();
    private long safraSelecionada = 0L;
    private long despesaId = 0L;
    private long categoriaPendente = 0L; // para marcar no modo edição após carregar chips
    private Despesa existente;
    private ComprovanteCampo comprovante;
    /** uuid do lançamento salvo (gerado aqui no cadastro novo, pra ligar o comprovante). */
    private String uuidSalvo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_despesa);
        vm = new ViewModelProvider(this).get(DespesaViewModel.class);

        edtDescricao = findViewById(R.id.edtDescricao);
        edtValor = findViewById(R.id.edtValor);
        edtData = findViewById(R.id.edtData);
        dropSafra = findViewById(R.id.dropSafra);
        chipsCategoria = findViewById(R.id.chipsCategoria);
        dropMaquina = findViewById(R.id.dropMaquina);
        tilMaquina = findViewById(R.id.tilMaquina);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        MaskUtils.aplicarData(edtData);
        comprovante = new ComprovanteCampo(this, Anexo.TIPO_DESPESA, false);
        tilMaquina.setVisibility(View.GONE);

        safraSelecionada = getIntent().getLongExtra(NavKeys.SAFRA_ID, 0L);
        despesaId = getIntent().getLongExtra(NavKeys.LANCAMENTO_ID, 0L);
        setTitle(despesaId == 0L ? R.string.nova_despesa_title : R.string.editar_despesa_title);

        if (despesaId == 0L) edtData.setText(DateUtils.hojeBr());

        long usuarioId = new SessionManager(this).getUsuarioId();
        vm.opcoesSafra(usuarioId).observe(this, this::popularSafras);
        vm.categorias().observe(this, this::popularChips);
        new MaquinaRepository(this).listar(usuarioId).observe(this, this::popularMaquinas);

        if (despesaId != 0L) {
            vm.obter(despesaId).observe(this, d -> {
                if (d != null && existente == null) {
                    existente = d;
                    comprovante.setLancamentoUuid(d.uuid);
                    safraSelecionada = d.safraId;
                    categoriaPendente = d.categoriaId;
                    maquinaPendente = d.maquinaId;
                    edtDescricao.setText(d.descricao);
                    edtValor.setText(String.valueOf(d.valor));
                    edtData.setText(DateUtils.isoParaBr(d.dataRegistro));
                    marcarSafraSelecionada();
                    marcarCategoria(d.categoriaId);
                    marcarMaquina(d.maquinaId);
                }
            });
        }

        dropSafra.setOnItemClickListener((parent, view, position, id) -> {
            String sel = parent.getItemAtPosition(position).toString();
            for (SafraOpcao o : opcoes) if (o.rotulo().equals(sel)) { safraSelecionada = o.id; return; }
        });

        dropMaquina.setOnItemClickListener((parent, view, position, id) -> {
            String sel = parent.getItemAtPosition(position).toString();
            if (sel.equals(getString(R.string.maquina_nenhuma))) { maquinaSelecionada = null; return; }
            for (Maquina m : maquinas) if (m.nome.equals(sel)) { maquinaSelecionada = m.id; return; }
            maquinaSelecionada = null;
        });

        btnSalvar.setOnClickListener(v -> salvar());
        vm.getSalvoComId().observe(this, id -> {
            if (id == null) return;
            comprovante.aplicar(uuidSalvo);
            android.content.Intent i = new android.content.Intent(this, SucessoActivity.class);
            i.putExtra(NavKeys.SUCESSO_MSG, getString(R.string.sucesso_despesa));
            startActivity(i);
            finish();
        });
    }

    private void popularChips(List<CategoriaDespesa> categorias) {
        chipsCategoria.removeAllViews();
        categoriasCarregadas.clear();
        if (categorias == null) { atualizarVisibilidadeMaquina(); return; }
        categoriasCarregadas.addAll(categorias);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (CategoriaDespesa c : categorias) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_categoria, chipsCategoria, false);
            chip.setId(View.generateViewId());
            chip.setText(c.nome);
            chip.setTag(c.id);
            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) atualizarVisibilidadeMaquina();
            });
            chipsCategoria.addView(chip);
        }
        if (categoriaPendente != 0L) marcarCategoria(categoriaPendente);
        atualizarVisibilidadeMaquina();
    }

    private void marcarCategoria(long categoriaId) {
        for (int i = 0; i < chipsCategoria.getChildCount(); i++) {
            Chip chip = (Chip) chipsCategoria.getChildAt(i);
            if (chip.getTag() != null && (long) chip.getTag() == categoriaId) {
                chipsCategoria.check(chip.getId());
                return;
            }
        }
    }

    /** Mostra o seletor de máquina só quando a categoria escolhida for "Máquinas"; caso contrário, esconde e limpa a seleção. */
    private void atualizarVisibilidadeMaquina() {
        boolean isMaquina = isCategoriaMaquina(categoriaSelecionada());
        tilMaquina.setVisibility(isMaquina ? View.VISIBLE : View.GONE);
        if (!isMaquina) {
            maquinaSelecionada = null;
            dropMaquina.setText("", false);
        }
    }

    private boolean isCategoriaMaquina(long categoriaId) {
        if (categoriaId == 0L) return false;
        for (CategoriaDespesa c : categoriasCarregadas) {
            if (c.id == categoriaId) return CATEGORIA_MAQUINAS.equalsIgnoreCase(c.nome);
        }
        return false;
    }

    private void popularSafras(List<SafraOpcao> lista) {
        opcoes.clear();
        if (lista != null) opcoes.addAll(lista);
        List<String> rotulos = new ArrayList<>();
        for (SafraOpcao o : opcoes) rotulos.add(o.rotulo());
        dropSafra.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rotulos));
        marcarSafraSelecionada();
    }

    private void marcarSafraSelecionada() {
        for (int i = 0; i < opcoes.size(); i++) {
            if (opcoes.get(i).id == safraSelecionada) {
                dropSafra.setText(opcoes.get(i).rotulo(), false);
                return;
            }
        }
    }

    private long categoriaSelecionada() {
        int checkedId = chipsCategoria.getCheckedChipId();
        if (checkedId == ChipGroup.NO_ID) return 0L;
        Chip chip = chipsCategoria.findViewById(checkedId);
        return (chip != null && chip.getTag() != null) ? (long) chip.getTag() : 0L;
    }

    private void salvar() {
        String desc = txt(edtDescricao);
        if (desc.isEmpty()) { toast(R.string.err_descricao_obrigatoria); return; }
        Double valor = MoneyUtils.parse(txt(edtValor));
        if (valor == null || valor <= 0) { toast(R.string.err_valor_obrigatorio); return; }
        long categoriaId = categoriaSelecionada();
        if (categoriaId == 0L) { toast(R.string.err_categoria_obrigatoria); return; }
        if (safraSelecionada == 0L) { toast(R.string.err_safra_obrigatoria); return; }

        Despesa d = (existente != null) ? existente : new Despesa();
        d.id = despesaId;
        d.safraId = safraSelecionada;
        d.categoriaId = categoriaId;
        d.maquinaId = maquinaSelecionada;
        d.descricao = desc;
        d.valor = valor;
        String iso = DateUtils.brParaIso(txt(edtData));
        d.dataRegistro = (iso != null) ? iso : DateUtils.hojeIso();
        if (d.uuid == null || d.uuid.isEmpty()) d.uuid = java.util.UUID.randomUUID().toString();
        uuidSalvo = d.uuid;
        vm.salvar(d);
    }

    private void toast(int res) { Toast.makeText(this, res, Toast.LENGTH_LONG).show(); }
    private String txt(TextInputEditText e) { return e.getText() == null ? "" : e.getText().toString().trim(); }

    private void popularMaquinas(java.util.List<Maquina> lista) {
        maquinas.clear();
        if (lista != null) maquinas.addAll(lista);
        java.util.List<String> nomes = new java.util.ArrayList<>();
        nomes.add(getString(R.string.maquina_nenhuma));
        for (Maquina m : maquinas) nomes.add(m.nome);
        dropMaquina.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomes));
        if (maquinaPendente != null) marcarMaquina(maquinaPendente);
        else if (maquinaSelecionada == null) dropMaquina.setText(getString(R.string.maquina_nenhuma), false);
    }

    private void marcarMaquina(Long maquinaId) {
        if (maquinaId == null) { dropMaquina.setText(getString(R.string.maquina_nenhuma), false); maquinaSelecionada = null; return; }
        for (Maquina m : maquinas) {
            if (m.id == maquinaId) { dropMaquina.setText(m.nome, false); maquinaSelecionada = m.id; return; }
        }
    }
}
