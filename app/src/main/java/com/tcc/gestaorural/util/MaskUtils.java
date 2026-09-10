package com.tcc.gestaorural.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Máscaras de input reutilizáveis (padrão do projeto).
 *
 * Uso:
 *   MaskUtils.aplicarCpf(edtCpf);
 *   MaskUtils.aplicarTelefone(edtTelefone);
 *   MaskUtils.aplicarData(edtNascimento);
 *
 * Para obter só os dígitos (ex.: salvar no banco sem pontuação):
 *   String somenteDigitos = MaskUtils.digitos(edt.getText().toString());
 */
public final class MaskUtils {

    private MaskUtils() {}

    public static final String MASK_CPF   = "###.###.###-##";
    public static final String MASK_DATA  = "##/##/####";
    // Telefone é tratado à parte (aceita 10 ou 11 dígitos).

    /** Remove tudo que não for dígito. */
    public static String digitos(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    /** Formata 11 dígitos como 000.000.000-00 (para exibição). Retorna o próprio valor se não tiver 11 dígitos. */
    public static String formatarCpf(String cpfDigitos) {
        String n = digitos(cpfDigitos);
        if (n.length() != 11) return cpfDigitos == null ? "" : cpfDigitos;
        return n.substring(0, 3) + "." + n.substring(3, 6) + "." + n.substring(6, 9) + "-" + n.substring(9);
    }

    // ---------- CPF / Data: máscara de tamanho fixo baseada em padrão ----------

    public static void aplicarCpf(EditText field) {
        field.addTextChangedListener(mascaraFixa(field, MASK_CPF));
    }

    public static void aplicarData(EditText field) {
        field.addTextChangedListener(mascaraFixa(field, MASK_DATA));
    }

    private static TextWatcher mascaraFixa(final EditText field, final String mask) {
        return new TextWatcher() {
            private boolean editando;

            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (editando) return;
                String nums = digitos(s.toString());
                StringBuilder out = new StringBuilder();
                int i = 0;
                for (int m = 0; m < mask.length() && i < nums.length(); m++) {
                    char ch = mask.charAt(m);
                    if (ch == '#') {
                        out.append(nums.charAt(i));
                        i++;
                    } else {
                        out.append(ch);
                    }
                }
                editando = true;
                field.setText(out.toString());
                field.setSelection(field.getText().length());
                editando = false;
            }
        };
    }

    // ---------- Telefone: (##) ####-####  ou  (##) #####-#### ----------

    public static void aplicarTelefone(final EditText field) {
        field.addTextChangedListener(new TextWatcher() {
            private boolean editando;

            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (editando) return;
                String n = digitos(s.toString());
                if (n.length() > 11) n = n.substring(0, 11);

                StringBuilder out = new StringBuilder();
                if (n.length() > 0) out.append("(").append(n, 0, Math.min(2, n.length()));
                if (n.length() >= 2) out.append(") ");
                if (n.length() > 2) {
                    // 9 dígitos (celular) -> 5 antes do traço; 8 dígitos (fixo) -> 4
                    int corte = (n.length() > 10) ? 7 : 6;
                    out.append(n, 2, Math.min(corte, n.length()));
                    if (n.length() > corte) {
                        out.append("-").append(n.substring(corte));
                    }
                }
                editando = true;
                field.setText(out.toString());
                field.setSelection(field.getText().length());
                editando = false;
            }
        });
    }
}
