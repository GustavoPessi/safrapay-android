package com.tcc.gestaorural.ui.comum;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.Periodo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Controla o layout view_seletor_periodo (incluído com &lt;include&gt;): lista suspensa Dia / Mês /
 * Ano / Intervalo (e "Tudo", se permitido), setas ‹ › pra andar e toque no texto pra escolher a data.
 * Feito pra ser reaproveitado (Relatório, Finanças e, depois, a tela de comparação).
 */
public class SeletorPeriodo {

    public interface Listener { void onPeriodoMudou(Periodo periodo); }

    private static final int ANO_MIN = 2000;

    private final Activity activity;
    private final TextView txtPeriodo;
    private final ImageButton btnAnterior, btnProximo;
    private final View linhaNavegacao, linhaIntervalo;
    private final EditText edtInicio, edtFim;
    private final MaterialAutoCompleteTextView dropTipo;
    private final List<Periodo.Tipo> tipos = new ArrayList<>();
    private Listener listener;
    private Periodo periodo;

    public SeletorPeriodo(Activity activity, View raiz, boolean permitirTudo, Periodo inicial) {
        this.activity = activity;
        dropTipo = raiz.findViewById(R.id.dropTipoPeriodo);
        txtPeriodo = raiz.findViewById(R.id.txtPeriodo);
        btnAnterior = raiz.findViewById(R.id.btnPeriodoAnterior);
        btnProximo = raiz.findViewById(R.id.btnPeriodoProximo);
        linhaNavegacao = raiz.findViewById(R.id.linhaNavegacao);
        linhaIntervalo = raiz.findViewById(R.id.linhaIntervalo);
        edtInicio = raiz.findViewById(R.id.edtPeriodoInicio);
        edtFim = raiz.findViewById(R.id.edtPeriodoFim);
        MaskUtils.aplicarData(edtInicio);
        MaskUtils.aplicarData(edtFim);

        if (permitirTudo) tipos.add(Periodo.Tipo.TUDO);
        tipos.add(Periodo.Tipo.DIA);
        tipos.add(Periodo.Tipo.MES);
        tipos.add(Periodo.Tipo.ANO);
        tipos.add(Periodo.Tipo.INTERVALO);
        List<String> nomes = new ArrayList<>();
        for (Periodo.Tipo t : tipos) nomes.add(activity.getString(nomeTipo(t)));
        dropTipo.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, nomes));

        btnAnterior.setOnClickListener(v -> mudar(periodo.andar(-1)));
        btnProximo.setOnClickListener(v -> mudar(periodo.andar(1)));
        txtPeriodo.setOnClickListener(v -> escolher());
        raiz.findViewById(R.id.btnPeriodoAplicar).setOnClickListener(v -> aplicarIntervalo());
        // "Concluído" no teclado da data final também aplica.
        edtFim.setOnEditorActionListener((v, acao, ev) -> {
            if (acao != EditorInfo.IME_ACTION_DONE) return false;
            aplicarIntervalo();
            return true;
        });

        periodo = inicial != null ? inicial : Periodo.atual(Periodo.Tipo.ANO);
        if (!permitirTudo && periodo.getTipo() == Periodo.Tipo.TUDO) periodo = Periodo.atual(Periodo.Tipo.ANO);
        if (periodo.getTipo() == Periodo.Tipo.INTERVALO) {
            edtInicio.setText(DateUtils.isoParaBr(periodo.inicio()));
            edtFim.setText(DateUtils.isoParaBr(periodo.fim()));
        }
        atualizarTela();

        dropTipo.setOnItemClickListener((parent, v, pos, id) -> {
            Periodo.Tipo tipo = tipos.get(pos);
            if (tipo == periodo.getTipo()) return;
            if (tipo == Periodo.Tipo.INTERVALO) {
                // Começa o intervalo com o que estava na tela (ou mês atual, se era "Tudo").
                Periodo base = periodo.getTipo() == Periodo.Tipo.TUDO ? Periodo.atual(Periodo.Tipo.INTERVALO)
                        : Periodo.intervalo(periodo.inicio(), periodo.fim());
                edtInicio.setText(DateUtils.isoParaBr(base.inicio()));
                edtFim.setText(DateUtils.isoParaBr(base.fim()));
                mudar(base);
            } else {
                mudar(Periodo.atual(tipo));
            }
        });
    }

    public void setListener(Listener l) { this.listener = l; }

    public Periodo getPeriodo() { return periodo; }

    /** Guarda o período no Bundle (rotação de tela / processo recriado). */
    public void salvar(android.os.Bundle out, String chave) {
        out.putString(chave + ".tipo", periodo.getTipo().name());
        out.putString(chave + ".ini", periodo.inicio());
        out.putString(chave + ".fim", periodo.fim());
    }

    /** Lê o que {@link #salvar} guardou; null se não houver. */
    public static Periodo restaurar(android.os.Bundle in, String chave) {
        if (in == null || in.getString(chave + ".tipo") == null) return null;
        return Periodo.restaurar(in.getString(chave + ".tipo"),
                in.getString(chave + ".ini"), in.getString(chave + ".fim"));
    }

    private static int nomeTipo(Periodo.Tipo tipo) {
        switch (tipo) {
            case TUDO: return R.string.periodo_tudo;
            case DIA: return R.string.periodo_dia;
            case MES: return R.string.periodo_mes;
            case ANO: return R.string.periodo_ano;
            default: return R.string.periodo_intervalo;
        }
    }

    private void mudar(Periodo novo) {
        periodo = novo;
        atualizarTela();
        if (listener != null) listener.onPeriodoMudou(periodo);
    }

    private void atualizarTela() {
        boolean intervalo = periodo.getTipo() == Periodo.Tipo.INTERVALO;
        boolean tudo = periodo.getTipo() == Periodo.Tipo.TUDO;
        linhaIntervalo.setVisibility(intervalo ? View.VISIBLE : View.GONE);
        linhaNavegacao.setVisibility(intervalo || tudo ? View.GONE : View.VISIBLE);
        txtPeriodo.setText(periodo.rotulo());
        dropTipo.setText(activity.getString(nomeTipo(periodo.getTipo())), false);
        String r = periodo.rotulo();
        btnAnterior.setContentDescription(activity.getString(R.string.periodo_anterior) + " (" + r + ")");
        btnProximo.setContentDescription(activity.getString(R.string.periodo_proximo) + " (" + r + ")");
    }

    private void aplicarIntervalo() {
        String ini = DateUtils.brParaIso(edtInicio.getText() == null ? "" : edtInicio.getText().toString());
        String fim = DateUtils.brParaIso(edtFim.getText() == null ? "" : edtFim.getText().toString());
        if (ini == null || fim == null) {
            Toast.makeText(activity, R.string.periodo_data_invalida, Toast.LENGTH_SHORT).show();
            return;
        }
        esconderTeclado();
        mudar(Periodo.intervalo(ini, fim));
    }

    /** Fecha o teclado e tira o foco dos campos de data depois de aplicar o intervalo. */
    private void esconderTeclado() {
        View foco = activity.getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(foco != null ? foco.getWindowToken() : edtFim.getWindowToken(), 0);
        edtInicio.clearFocus();
        edtFim.clearFocus();
    }

    /** Toque no texto: calendário (dia) ou roletas de mês/ano. */
    private void escolher() {
        switch (periodo.getTipo()) {
            case DIA:
                new DatePickerDialog(activity, (v, a, m, d) -> mudar(Periodo.dia(a, m + 1, d)),
                        periodo.getAno(), periodo.getMes() - 1, periodo.getDia()).show();
                break;
            case MES: escolherMesAno(true); break;
            case ANO: escolherMesAno(false); break;
            default: break;
        }
    }

    private void escolherMesAno(boolean comMes) {
        int anoMax = Calendar.getInstance().get(Calendar.YEAR) + 5;
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(android.view.Gravity.CENTER);
        NumberPicker npMes = new NumberPicker(activity);
        if (comMes) {
            String[] nomes = new String[12];
            for (int i = 0; i < 12; i++) nomes[i] = Periodo.nomeMes(i + 1);
            npMes.setMinValue(1);
            npMes.setMaxValue(12);
            npMes.setDisplayedValues(nomes);
            npMes.setValue(periodo.getMes());
            box.addView(npMes);
        }
        NumberPicker npAno = new NumberPicker(activity);
        npAno.setMinValue(ANO_MIN);
        npAno.setMaxValue(Math.max(anoMax, periodo.getAno()));
        npAno.setValue(Math.max(ANO_MIN, periodo.getAno()));
        npAno.setWrapSelectorWheel(false);
        box.addView(npAno);

        new AlertDialog.Builder(activity)
                .setTitle(comMes ? R.string.periodo_escolher_mes : R.string.periodo_escolher_ano)
                .setView(box)
                .setPositiveButton(android.R.string.ok, (d, w) -> mudar(comMes
                        ? Periodo.mes(npAno.getValue(), npMes.getValue())
                        : Periodo.ano(npAno.getValue())))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
