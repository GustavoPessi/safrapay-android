package com.tcc.gestaorural.ui.safra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.model.SafraResumo;

import java.util.ArrayList;
import java.util.List;

public class SafraAdapter extends RecyclerView.Adapter<SafraAdapter.VH> {

    public interface OnItemClick { void clicou(SafraResumo s); }

    private final List<SafraResumo> itens = new ArrayList<>();
    private final OnItemClick listener;

    public SafraAdapter(OnItemClick l) { this.listener = l; }

    public void submeter(List<SafraResumo> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_safra, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SafraResumo s = itens.get(pos);
        h.nome.setText((s.culturaNome == null || s.culturaNome.trim().isEmpty())
                ? h.itemView.getContext().getString(R.string.safra_sem_cultura) : s.culturaNome);
        String info = s.anoSafra;
        if (s.variedade != null && !s.variedade.isEmpty()) info += "  •  " + s.variedade;
        h.info.setText(info);
        h.itemView.setOnClickListener(v -> listener.clicou(s));
    }

    @Override public int getItemCount() { return itens.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nome, info;
        VH(@NonNull View v) { super(v); nome = v.findViewById(R.id.txtNome); info = v.findViewById(R.id.txtInfo); }
    }
}
