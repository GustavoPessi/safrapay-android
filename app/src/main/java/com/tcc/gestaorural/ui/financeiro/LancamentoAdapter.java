package com.tcc.gestaorural.ui.financeiro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.model.Lancamento;
import com.tcc.gestaorural.util.DateUtils;
import com.tcc.gestaorural.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class LancamentoAdapter extends RecyclerView.Adapter<LancamentoAdapter.VH> {

    public interface OnItemClick { void clicou(Lancamento l); }

    private final List<Lancamento> itens = new ArrayList<>();
    private final OnItemClick listener;

    public LancamentoAdapter(OnItemClick l) { this.listener = l; }

    public void submeter(List<Lancamento> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lancamento, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Lancamento l = itens.get(pos);
        boolean receita = l.isReceita();
        h.descricao.setText(l.descricao);
        h.data.setText(DateUtils.isoParaBr(l.data));
        h.valor.setText(MoneyUtils.comSinal(l.valor, receita));
        int cor = ContextCompat.getColor(h.itemView.getContext(), receita ? R.color.receita : R.color.despesa);
        h.valor.setTextColor(cor);
        h.img.setImageResource(receita ? R.drawable.ic_receita : R.drawable.ic_despesa);
        h.itemView.setOnClickListener(v -> listener.clicou(l));
    }

    @Override public int getItemCount() { return itens.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView img; final TextView descricao, data, valor;
        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgTipo);
            descricao = v.findViewById(R.id.txtDescricao);
            data = v.findViewById(R.id.txtData);
            valor = v.findViewById(R.id.txtValor);
        }
    }
}
