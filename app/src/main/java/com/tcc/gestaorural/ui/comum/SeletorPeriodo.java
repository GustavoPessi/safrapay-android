package com.tcc.gestaorural.ui.comum;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.ChipGroup;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MaskUtils;
import com.tcc.gestaorural.util.Periodo;

import java.util.Calendar;

/**
 * Controla o layout view_seletor_periodo (incluído com &lt;include&gt;): chips Dia / Mês / Ano /
 * Intervalo (e "Tudo", se permitido), setas ‹ › pra andar e toque no texto pra escolher a data.
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
    private final ChipGroup chips;
    private Listener listener;
    private Periodo periodo;
    private boolean montando;

    public SeletorPeriodo(Activity activity, View raiz, boolean permitirTudo, Periodo inicial) {
        this.activity = activity;
        chips = raiz.findViewById(R.id.chipsPeriodo);
        txtPeriodo = raiz.findViewById(R.id.txtPeriodo);
        btnAnterior = raiz.findViewById(R.id.btnPeriodoAnterior);
        btnProximo = raiz.findViewById(R.id.btnPeriodoProximo);
        linhaNavegacao = raiz.findViewById(R.id.linhaNavegacao);
        linhaIntervalo = raiz.findViewById(R.id.linhaIntervalo);
        edtInicio = raiz.findViewById(R.id.edtPeriodoInicio);
        edtFim = raiz.findViewById(R.id.edtPeriodoFim);
        MaskUtils.aplicarData(edtInicio);
        MaskUtils.aplicarData(edtFim);

        raiz.findViewById(R.id.chipPeriodoTudo).setVisibility(permitirTudo ? View.VISIBLE : View.GONE);

        btnAnterior.setOnClickListener(v -> mudar(periodo.andar(-1)));
        btnProximo.setOnClickListener(v -> mudar(periodo.andar(1)));
        txtPeriodo.setOnClickListener(v -> escolher());
        raiz.findViewById(R.id.btnPeriodoAplicar).setOnClickListener(v -> aplicarIntervalo());

        periodo = inicial != null ? inicial : Periodo.atual(Periodo.Tipo.ANO);
        if (!permitirTudo && periodo.getTipo() == Periodo.Tipo.TUDO) periodo = Periodo.atual(Periodo.Tipo.ANO);
        if (periodo.getTipo() == Periodo.Tipo.INTERVALO) {
            edtInicio.setText(DateUtils.isoParaBr(periodo.inicio()));
            edtFim.setText(DateUtils.isoParaBr(periodo.fim()));
        }
        montando = true;
        chips.check(idChip(periodo.getTipo()));
        montando = false;
        atualizarTela();

        chips.setOnCheckedStateChangeListener((g, ids) -> {
            if (montando || ids.isEmpty()) return;
            Periodo.Tipo tipo = tipoDoChip(ids.get(0));
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

    /** Ids fixos por tipo (chips do view_seletor_periodo.xml), estáveis entre recriações. */
    private static int idChip(Periodo.Tipo tipo) {
        switch (tipo) {
            case TUDO: return R.id.chipPeriodoTudo;
            case DIA: return R.id.chipPeriodoDia;
            case MES: return R.id.chipPeriodoMes;
            case ANO: return R.id.chipPeriodoAno;
            default: return R.id.chipPeriodoIntervalo;
        }
    }

    private static Periodo.Tipo tipoDoChip(int id) {
        if (id == R.id.chipPeriodoTudo) return Periodo.Tipo.TUDO;
        if (id == R.id.chipPeriodoDia) return Periodo.Tipo.DIA;
        if (id == R.id.chipPeriodoMes) return Periodo.Tipo.MES;
        if (id == R.id.chipPeriodoAno) return Periodo.Tipo.ANO;
        return Periodo.Tipo.INTERVALO;
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
        mudar(Periodo.intervalo(ini, fim));
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
