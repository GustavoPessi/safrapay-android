package com.tcc.gestaorural.ui.metas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.metas.MetaViewModel.MetaProgresso;
import com.tcc.gestaorural.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class MetaAdapter extends RecyclerView.Adapter<MetaAdapter.VH> {

    public interface OnItemClick { void clicou(MetaProgresso m); }

    private final List<MetaProgresso> itens = new ArrayList<>();
    private final OnItemClick listener;

    public MetaAdapter(OnItemClick l) { this.listener = l; }

    public void submeter(List<MetaProgresso> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meta, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MetaProgresso mp = itens.get(pos);
        h.descricao.setText(mp.meta.descricao);
        h.propriedade.setText(mp.meta.safraLabel);
        h.progresso.setProgressCompat(mp.percentual, false);
        String texto = h.itemView.getContext().getString(R.string.meta_de,
                MoneyUtils.formatar(mp.valorAtual), MoneyUtils.formatar(mp.meta.valorAlvo))
                + " · " + mp.percentual + "%";
        h.textoProgresso.setText(texto);
        h.textoProgresso.setTextColor(androidx.core.content.ContextCompat.getColor(h.itemView.getContext(),
                mp.valorAtual < 0 ? R.color.despesa : R.color.text_secondary));
        h.itemView.setOnClickListener(v -> listener.clicou(mp));
    }

    @Override public int getItemCount() { return itens.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView descricao, propriedade, textoProgresso;
        final LinearProgressIndicator progresso;
        VH(@NonNull View v) {
            super(v);
            descricao = v.findViewById(R.id.txtDescricao);
            propriedade = v.findViewById(R.id.txtPropriedade);
            textoProgresso = v.findViewById(R.id.txtProgresso);
            progresso = v.findViewById(R.id.progresso);
        }
    }
}
