package com.tcc.gestaorural.ui.cultura;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Cultura;

import java.util.ArrayList;
import java.util.List;

public class CulturaAdapter extends RecyclerView.Adapter<CulturaAdapter.VH> {
    public interface OnItemClick { void clicou(Cultura c); }
    private final List<Cultura> itens = new ArrayList<>();
    private final OnItemClick listener;
    public CulturaAdapter(OnItemClick l) { this.listener = l; }
    public void submeter(List<Cultura> novos) { itens.clear(); if (novos != null) itens.addAll(novos); notifyDataSetChanged(); }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cultura, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Cultura c = itens.get(pos);
        h.nome.setText(c.nome);
        h.itemView.setOnClickListener(v -> listener.clicou(c));
    }
    @Override public int getItemCount() { return itens.size(); }
    static class VH extends RecyclerView.ViewHolder {
        final TextView nome;
        VH(@NonNull View v) { super(v); nome = v.findViewById(R.id.txtNome); }
    }
}
