package com.tcc.gestaorural.ui.maquina;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.data.local.entity.Maquina;
import com.tcc.gestaorural.ui.maquina.MaquinaViewModel.MaquinaComGasto;
import com.tcc.gestaorural.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class MaquinaAdapter extends RecyclerView.Adapter<MaquinaAdapter.VH> {

    public interface OnItemClick { void clicou(Maquina m); }

    private final List<MaquinaComGasto> itens = new ArrayList<>();
    private final OnItemClick listener;

    public MaquinaAdapter(OnItemClick l) { this.listener = l; }

    public void submeter(List<MaquinaComGasto> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maquina, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MaquinaComGasto mg = itens.get(pos);
        Maquina m = mg.maquina;
        h.nome.setText(m.nome);
        StringBuilder info = new StringBuilder();
        if (m.marca != null && !m.marca.isEmpty()) info.append(m.marca);
        if (m.ano != null && !m.ano.isEmpty()) {
            if (info.length() > 0) info.append(" • ");
            info.append(m.ano);
        }
        h.info.setText(info.toString());
        h.gasto.setText(h.itemView.getContext().getString(R.string.maq_gasto_total, MoneyUtils.formatar(mg.gasto)));
        h.itemView.setOnClickListener(v -> listener.clicou(m));
    }

    @Override public int getItemCount() { return itens.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nome, info, gasto;
        VH(@NonNull View v) {
            super(v);
            nome = v.findViewById(R.id.txtNome);
            info = v.findViewById(R.id.txtInfo);
            gasto = v.findViewById(R.id.txtGasto);
        }
    }
}
