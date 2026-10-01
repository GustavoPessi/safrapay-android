package com.tcc.gestaorural.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemResponseDto;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Acao {
        void confirmar(ChatItem item);
        void descartar(ChatItem item);
    }

    private final List<ChatItem> itens = new ArrayList<>();
    private final Acao acao;

    public ChatAdapter(Acao acao) { this.acao = acao; }

    public void submeter(List<ChatItem> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) { return itens.get(position).tipo; }
    @Override public int getItemCount() { return itens.size(); }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == ChatItem.USUARIO) {
            return new VHTexto(inf.inflate(R.layout.item_chat_usuario, parent, false));
        } else if (viewType == ChatItem.PROPOSTA) {
            return new VHProposta(inf.inflate(R.layout.item_chat_proposta, parent, false));
        }
        return new VHTexto(inf.inflate(R.layout.item_chat_assistente, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatItem item = itens.get(position);
        if (holder instanceof VHTexto) {
            ((VHTexto) holder).txt.setText(item.texto);
        } else if (holder instanceof VHProposta) {
            ((VHProposta) holder).bind(item);
        }
    }

    static class VHTexto extends RecyclerView.ViewHolder {
        final TextView txt;
        VHTexto(@NonNull View v) { super(v); txt = v.findViewById(R.id.txtTexto); }
    }

    class VHProposta extends RecyclerView.ViewHolder {
        final TextView resumo, rodape;
        final View rowBotoes;
        final Button btnConfirmar, btnDescartar;

        VHProposta(@NonNull View v) {
            super(v);
            resumo = v.findViewById(R.id.txtResumo);
            rodape = v.findViewById(R.id.txtRodape);
            rowBotoes = v.findViewById(R.id.rowBotoes);
            btnConfirmar = v.findViewById(R.id.btnConfirmar);
            btnDescartar = v.findViewById(R.id.btnDescartar);
        }

        void bind(ChatItem item) {
            resumo.setText(montarResumo(item.lancamentos));
            if (item.resolvido) {
                rowBotoes.setVisibility(View.GONE);
                rodape.setVisibility(View.VISIBLE);
                rodape.setText(item.rodape);
            } else {
                rowBotoes.setVisibility(View.VISIBLE);
                rodape.setVisibility(View.GONE);
                btnConfirmar.setOnClickListener(v -> acao.confirmar(item));
                btnDescartar.setOnClickListener(v -> acao.descartar(item));
            }
        }
    }

    private static String montarResumo(List<ChatMensagemResponseDto.Lancamento> lancamentos) {
        StringBuilder sb = new StringBuilder();
        if (lancamentos == null) return "";
        for (int i = 0; i < lancamentos.size(); i++) {
            ChatMensagemResponseDto.Lancamento l = lancamentos.get(i);
            boolean receita = "RECEITA".equalsIgnoreCase(l.tipo);
            sb.append("• ").append(receita ? "Receita" : "Despesa");
            sb.append(" — ").append(l.valor != null ? MoneyUtils.formatar(l.valor) : "—");
            if (l.descricao != null && !l.descricao.isEmpty()) sb.append(" — ").append(l.descricao);
            if (!receita && l.categoria != null && !l.categoria.isEmpty()) sb.append(" — ").append(l.categoria);
            if (l.data != null && !l.data.isEmpty()) sb.append(" — ").append(DateUtils.isoParaBr(l.data));
            if (i < lancamentos.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
