package com.tcc.gestaorural.export;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tcc.gestaorural.data.local.AppDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * "Exportar meus dados" (Configurações) — portabilidade (LGPD, art. 18): uma planilha .xlsx com
 * tudo que a conta tem no aparelho (inclusive o que ainda não sincronizou), uma aba por assunto.
 * As fotos dos comprovantes não vão (só a lista deles); ficam no aparelho e saem pelo
 * "Compartilhar" de cada lançamento. Chamar gerar() fora da main thread.
 */
public final class ExportadorDados {

    private ExportadorDados() {}

    private static final String SAFRA_DESC =
            "COALESCE(t.cultura, 'Sem cultura') || ' • ' || s.ano_safra || ' • Talhão ' || t.nome";
    private static final String JOIN_SAFRA =
            " JOIN safra s ON s.id = x.safra_id JOIN talhao t ON t.id = s.talhao_id JOIN propriedade p ON p.id = t.propriedade_id ";

    public static File gerar(Context ctx, long usuarioId) throws IOException {
        SupportSQLiteDatabase db = AppDatabase.getInstance(ctx).getOpenHelper().getReadableDatabase();
        Object[] u = { usuarioId };
        String agora = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR")).format(new Date());

        PlanilhaXlsx p = new PlanilhaXlsx();
        p.abaTexto("Sobre", sobre(db, usuarioId, agora));
        tabela(p, db, "Perfil",
                "SELECT nome AS \"Nome\", email AS \"E-mail\", cpf AS \"CPF\", telefone AS \"Telefone\", "
                + "nascimento AS \"Nascimento\", criado_em AS \"Conta criada em\" FROM usuario WHERE id = ?", u);
        tabela(p, db, "Propriedades",
                "SELECT nome AS \"Nome\", localidade AS \"Localidade\", area_total AS \"Área total (ha)\", "
                + "tipo_solo AS \"Tipo de solo\", matricula AS \"Matrícula\", cib AS \"CIB\", valor AS \"Valor\", "
                + "latitude AS \"Latitude\", longitude AS \"Longitude\", observacoes AS \"Observações\", "
                + "criado_em AS \"Criado em\", uuid AS \"Identificador\" FROM propriedade WHERE usuario_id = ? ORDER BY nome", u);
        tabela(p, db, "Talhões",
                "SELECT p.nome AS \"Propriedade\", t.nome AS \"Talhão\", t.area_ha AS \"Área (ha)\", t.cultura AS \"Cultura\", "
                + "t.latitude AS \"Latitude\", t.longitude AS \"Longitude\", t.criado_em AS \"Criado em\", t.uuid AS \"Identificador\" "
                + "FROM talhao t JOIN propriedade p ON p.id = t.propriedade_id WHERE p.usuario_id = ? ORDER BY p.nome, t.nome", u);
        tabela(p, db, "Safras",
                "SELECT p.nome AS \"Propriedade\", t.nome AS \"Talhão\", t.cultura AS \"Cultura\", s.ano_safra AS \"Ano safra\", "
                + "s.variedade AS \"Variedade\", s.data_inicio AS \"Início\", s.data_fim AS \"Fim\", "
                + "CASE WHEN s.data_fim IS NULL OR s.data_fim = '' THEN 'Ativa' ELSE 'Encerrada' END AS \"Situação\", "
                + "s.uuid AS \"Identificador\" FROM safra s JOIN talhao t ON t.id = s.talhao_id "
                + "JOIN propriedade p ON p.id = t.propriedade_id WHERE p.usuario_id = ? ORDER BY s.ano_safra DESC", u);
        tabela(p, db, "Receitas",
                "SELECT x.data_registro AS \"Data\", x.descricao AS \"Descrição\", x.valor AS \"Valor\", "
                + SAFRA_DESC + " AS \"Safra\", p.nome AS \"Propriedade\", "
                + "CASE x.sincronizado WHEN 1 THEN 'Sim' ELSE 'Não' END AS \"Enviado ao servidor\", x.uuid AS \"Identificador\" "
                + "FROM receita x" + JOIN_SAFRA + "WHERE p.usuario_id = ? ORDER BY x.data_registro DESC", u);
        tabela(p, db, "Despesas",
                "SELECT x.data_registro AS \"Data\", x.descricao AS \"Descrição\", c.nome AS \"Categoria\", x.valor AS \"Valor\", "
                + "m.nome AS \"Máquina\", " + SAFRA_DESC + " AS \"Safra\", p.nome AS \"Propriedade\", "
                + "CASE x.sincronizado WHEN 1 THEN 'Sim' ELSE 'Não' END AS \"Enviado ao servidor\", x.uuid AS \"Identificador\" "
                + "FROM despesa x" + JOIN_SAFRA + "LEFT JOIN categoria_despesa c ON c.id = x.categoria_id "
                + "LEFT JOIN maquina m ON m.id = x.maquina_id WHERE p.usuario_id = ? ORDER BY x.data_registro DESC", u);
        tabela(p, db, "Metas",
                "SELECT x.descricao AS \"Descrição\", x.valor_alvo AS \"Valor alvo\", x.prazo AS \"Prazo\", "
                + SAFRA_DESC + " AS \"Safra\", p.nome AS \"Propriedade\", x.uuid AS \"Identificador\" "
                + "FROM meta_financeira x" + JOIN_SAFRA + "WHERE p.usuario_id = ?", u);
        tabela(p, db, "Máquinas",
                "SELECT nome AS \"Nome\", tipo AS \"Tipo\", marca AS \"Marca\", modelo AS \"Modelo\", ano AS \"Ano\", "
                + "identificacao AS \"Identificação\", valor AS \"Valor\", observacoes AS \"Observações\", uuid AS \"Identificador\" "
                + "FROM maquina WHERE usuario_id = ? ORDER BY nome", u);
        tabela(p, db, "Tarefas das máquinas",
                "SELECT m.nome AS \"Máquina\", a.texto AS \"Tarefa / anotação\", a.data AS \"Data\", a.prazo AS \"Prazo\", "
                + "CASE a.concluida WHEN 1 THEN 'Realizada' ELSE 'Pendente' END AS \"Situação\", "
                + "a.responsavel_nome AS \"Responsável\", a.concluido_por_nome AS \"Feita por\", "
                + "a.criado_por_nome AS \"Criada por\", a.uuid AS \"Identificador\" "
                + "FROM anotacao_maquina a JOIN maquina m ON m.id = a.maquina_id WHERE m.usuario_id = ? ORDER BY m.nome", u);
        tabela(p, db, "Comprovantes",
                "SELECT CASE an.lancamento_tipo WHEN 'RECEITA' THEN 'Receita' ELSE 'Despesa' END AS \"Tipo\", "
                + "COALESCE(d.descricao, r.descricao) AS \"Lançamento\", COALESCE(d.valor, r.valor) AS \"Valor\", "
                + "COALESCE(d.data_registro, r.data_registro) AS \"Data do lançamento\", "
                + "CASE an.origem WHEN 'CHAT_FOTO' THEN 'Foto no assistente' ELSE 'Anexado manualmente' END AS \"Origem\", "
                + "an.criado_em AS \"Guardado em\" FROM anexo an "
                + "LEFT JOIN despesa d ON an.lancamento_tipo = 'DESPESA' AND d.uuid = an.lancamento_uuid "
                + "LEFT JOIN receita r ON an.lancamento_tipo = 'RECEITA' AND r.uuid = an.lancamento_uuid "
                + "WHERE COALESCE(d.safra_id, r.safra_id) IN (SELECT s.id FROM safra s JOIN talhao t ON t.id = s.talhao_id "
                + "JOIN propriedade p ON p.id = t.propriedade_id WHERE p.usuario_id = ?) ORDER BY an.criado_em DESC", u);

        File pasta = new File(ctx.getCacheDir(), "exports"); // mesma pasta do relatório (file_paths.xml)
        if (!pasta.exists() && !pasta.mkdirs()) throw new IOException("Não foi possível criar a pasta de exportação");
        String dia = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        File arquivo = new File(pasta, "safrapay-meus-dados_" + dia + ".xlsx");
        try (FileOutputStream out = new FileOutputStream(arquivo)) { p.escrever(out); }
        return arquivo;
    }

    private static List<String> sobre(SupportSQLiteDatabase db, long usuarioId, String agora) {
        String nome = "", email = "";
        try (Cursor c = db.query("SELECT nome, email FROM usuario WHERE id = ?", new Object[]{ usuarioId })) {
            if (c.moveToFirst()) { nome = c.getString(0); email = c.getString(1); }
        }
        List<String> l = new ArrayList<>();
        l.add("SafraPay — Meus dados");
        l.add("Titular: " + nome + " (" + email + ")");
        l.add("Gerado em: " + agora);
        l.add("");
        l.add("Cópia dos dados da sua conta guardados neste aparelho, inclusive o que ainda não foi enviado ao servidor");
        l.add("(coluna \"Enviado ao servidor\" nas receitas e despesas). Pedido de portabilidade previsto na LGPD (Lei 13.709/2018, art. 18).");
        l.add("");
        l.add("Abas:");
        l.add("• Perfil, Propriedades, Talhões, Safras, Receitas, Despesas, Metas, Máquinas, Tarefas das máquinas e Comprovantes.");
        l.add("• \"Identificador\" é o código único de cada registro (o mesmo usado na sincronização com o servidor).");
        l.add("• As fotos dos comprovantes não vão nesta planilha (só a lista). Elas ficam no aparelho e podem ser compartilhadas na tela de cada lançamento.");
        l.add("");
        l.add(Formatos.AVISO_GERENCIAL);
        return l;
    }

    /** Uma aba a partir de um SELECT: os nomes das colunas (apelidos) viram o cabeçalho. */
    private static void tabela(PlanilhaXlsx p, SupportSQLiteDatabase db, String aba, String sql, Object[] args) {
        try (Cursor c = db.query(sql, args)) {
            String[] cab = c.getColumnNames();
            boolean[] moeda = new boolean[cab.length];
            for (int i = 0; i < cab.length; i++) moeda[i] = cab[i].startsWith("Valor");
            List<Object[]> linhas = new ArrayList<>();
            while (c.moveToNext()) {
                Object[] l = new Object[cab.length];
                for (int i = 0; i < cab.length; i++) {
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL: l[i] = null; break;
                        case Cursor.FIELD_TYPE_INTEGER: l[i] = c.getLong(i); break;
                        case Cursor.FIELD_TYPE_FLOAT: l[i] = c.getDouble(i); break;
                        default: l[i] = dataBr(c.getString(i));
                    }
                }
                linhas.add(l);
            }
            p.aba(aba, cab, linhas, moeda);
        }
    }

    /** "2026-10-08" → "08/10/2026"; "2026-10-08 14:30:00" → "08/10/2026 14:30". Outros textos ficam iguais. */
    static String dataBr(String s) {
        if (s == null) return null;
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return s.substring(8, 10) + "/" + s.substring(5, 7) + "/" + s.substring(0, 4);
        if (s.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}(:\\d{2})?"))
            return s.substring(8, 10) + "/" + s.substring(5, 7) + "/" + s.substring(0, 4) + " " + s.substring(11, 16);
        return s;
    }

    public static void compartilhar(Activity activity, File arquivo) {
        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", arquivo);
        Intent envio = new Intent(Intent.ACTION_SEND);
        envio.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        envio.putExtra(Intent.EXTRA_STREAM, uri);
        envio.putExtra(Intent.EXTRA_SUBJECT, "SafraPay — meus dados");
        envio.setClipData(ClipData.newRawUri(arquivo.getName(), uri));
        envio.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(envio, "Enviar meus dados"));
    }
}
