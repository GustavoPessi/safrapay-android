package com.tcc.gestaorural.ui.talhao;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Talhao;

import java.util.ArrayList;
import java.util.List;

public class TalhaoAdapter extends RecyclerView.Adapter<TalhaoAdapter.VH> {

    public interface OnItemClick { void clicou(Talhao t); }

    private final List<Talhao> itens = new ArrayList<>();
    private final OnItemClick listener;

    public TalhaoAdapter(OnItemClick l) { this.listener = l; }

    public void submeter(List<Talhao> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_talhao, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Talhao t = itens.get(pos);
        h.nome.setText(t.nome);
        h.info.setText(t.areaHa == null ? "" :
                h.itemView.getContext().getString(R.string.fmt_area_ha, String.valueOf(t.areaHa)));
        h.itemView.setOnClickListener(v -> listener.clicou(t));
    }

    @Override public int getItemCount() { return itens.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nome, info;
        VH(@NonNull View v) { super(v); nome = v.findViewById(R.id.txtNome); info = v.findViewById(R.id.txtInfo); }
    }
}
