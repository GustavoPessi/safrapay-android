package com.tcc.gestaorural.ui.chat;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.remote.dto.ChatMensagemResponseDto;
import com.tcc.gestaorural.data.repository.ChatConsultaExecutor;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Acao {
        void confirmar(ChatItem item);
        void descartar(ChatItem item);
        void sugestao(String texto);
    }

    private final List<ChatItem> itens = new ArrayList<>();
    private final Acao acao;

    public ChatAdapter(Acao acao) { this.acao = acao; }

    public void submeter(List<ChatItem> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) { return itens.get(position).getTipo(); }
    @Override public int getItemCount() { return itens.size(); }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ChatItem.USUARIO:   return new VHTexto(inf.inflate(R.layout.item_chat_usuario, parent, false));
            case ChatItem.PROPOSTA:  return new VHProposta(inf.inflate(R.layout.item_chat_proposta, parent, false));
            case ChatItem.RESULTADO: return new VHResultado(inf.inflate(R.layout.item_chat_resultado, parent, false));
            case ChatItem.SUGESTOES: return new VHSugestoes(inf.inflate(R.layout.item_chat_sugestoes, parent, false));
            default:                 return new VHTexto(inf.inflate(R.layout.item_chat_assistente, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatItem item = itens.get(position);
        if (holder instanceof VHTexto) ((VHTexto) holder).txt.setText(item.getTexto());
        else if (holder instanceof VHProposta) ((VHProposta) holder).bind(item);
        else if (holder instanceof VHResultado) ((VHResultado) holder).bind(item.getResultado());
        else if (holder instanceof VHSugestoes) ((VHSugestoes) holder).bind(item.getSugestoes());
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
            resumo.setText(montarResumo(item.getLancamentos()));
            if (item.isResolvido()) {
                rowBotoes.setVisibility(View.GONE);
                rodape.setVisibility(View.VISIBLE);
                rodape.setText(item.getRodape());
            } else {
                rowBotoes.setVisibility(View.VISIBLE);
                rodape.setVisibility(View.GONE);
                btnConfirmar.setOnClickListener(v -> acao.confirmar(item));
                btnDescartar.setOnClickListener(v -> acao.descartar(item));
            }
        }
    }

    static class VHResultado extends RecyclerView.ViewHolder {
        final TextView titulo, observacao, linhas, total;

        VHResultado(@NonNull View v) {
            super(v);
            titulo = v.findViewById(R.id.txtTitulo);
            observacao = v.findViewById(R.id.txtObservacao);
            linhas = v.findViewById(R.id.txtLinhas);
            total = v.findViewById(R.id.txtTotal);
        }

        void bind(ChatConsultaExecutor.Resultado r) {
            if (r == null) return;
            titulo.setText(r.getTitulo());
            observacao.setVisibility(r.getObservacao() == null ? View.GONE : View.VISIBLE);
            observacao.setText(r.getObservacao());
            linhas.setText(android.text.TextUtils.join("\n", r.getLinhas()));
            total.setVisibility(r.getTotal() == null ? View.GONE : View.VISIBLE);
            total.setText(r.getTotal());
        }
    }

    class VHSugestoes extends RecyclerView.ViewHolder {
        final ChipGroup grupo;

        VHSugestoes(@NonNull View v) { super(v); grupo = v.findViewById(R.id.grupoSugestoes); }

        void bind(List<String> sugestoes) {
            grupo.removeAllViews();
            if (sugestoes == null) return;
            for (String s : sugestoes) {
                Chip chip = new Chip(grupo.getContext());
                chip.setText(s);
                chip.setCheckable(false);
                chip.setClickable(true);
                chip.setChipBackgroundColor(ColorStateList.valueOf(
                        ContextCompat.getColor(grupo.getContext(), R.color.green_100)));
                chip.setTextColor(ContextCompat.getColor(grupo.getContext(), R.color.green_900));
                chip.setOnClickListener(v -> acao.sugestao(s));
                grupo.addView(chip);
            }
        }
    }

    private static String montarResumo(List<ChatMensagemResponseDto.Lancamento> lancamentos) {
        StringBuilder sb = new StringBuilder();
        if (lancamentos == null) return "";
        for (int i = 0; i < lancamentos.size(); i++) {
            ChatMensagemResponseDto.Lancamento l = lancamentos.get(i);
            boolean receita = "RECEITA".equalsIgnoreCase(l.getTipo());
            sb.append("• ").append(receita ? "Receita" : "Despesa");
            sb.append(" — ").append(l.getValor() != null ? MoneyUtils.formatar(l.getValor()) : "—");
            if (l.getDescricao() != null && !l.getDescricao().isEmpty()) sb.append(" — ").append(l.getDescricao());
            if (!receita && l.getCategoria() != null && !l.getCategoria().isEmpty()) sb.append(" — ").append(l.getCategoria());
            if (l.getData() != null && !l.getData().isEmpty()) sb.append(" — ").append(DateUtils.isoParaBr(l.getData()));
            if (i < lancamentos.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
