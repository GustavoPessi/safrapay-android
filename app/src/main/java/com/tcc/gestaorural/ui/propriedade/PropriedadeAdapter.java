package com.tcc.gestaorural.ui.propriedade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Propriedade;

import java.util.ArrayList;
import java.util.List;

public class PropriedadeAdapter extends RecyclerView.Adapter<PropriedadeAdapter.VH> {

    public interface OnItemClick { void clicou(Propriedade p); }

    private final List<Propriedade> itens = new ArrayList<>();
    private final OnItemClick listener;

    public PropriedadeAdapter(OnItemClick listener) { this.listener = listener; }

    public void submeter(List<Propriedade> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_propriedade, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Propriedade p = itens.get(pos);
        h.nome.setText(p.nome);
        String info = p.localidade != null ? p.localidade : "";
        if (p.areaTotal != null) {
            String area = h.itemView.getContext().getString(R.string.fmt_area_ha,
                    String.valueOf(p.areaTotal));
            info = info.isEmpty() ? area : info + " · " + area;
        }
        h.info.setText(info);
        h.itemView.setOnClickListener(v -> listener.clicou(p));
    }

    @Override public int getItemCount() { return itens.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nome, info;
        VH(@NonNull View v) {
            super(v);
            nome = v.findViewById(R.id.txtNome);
            info = v.findViewById(R.id.txtInfo);
        }
    }
}
