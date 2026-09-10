package com.tcc.gestaorural.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tcc.gestaorural.R;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageVH> {

    private final int[] imgs = { R.drawable.ic_onb_1, R.drawable.ic_onb_2, R.drawable.ic_onb_3 };
    private final int[] titulos = { R.string.onb_1_title, R.string.onb_2_title, R.string.onb_3_title };
    private final int[] descs = { R.string.onb_1_desc, R.string.onb_2_desc, R.string.onb_3_desc };

    @NonNull @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false);
        return new PageVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH h, int position) {
        h.img.setImageResource(imgs[position]);
        h.titulo.setText(titulos[position]);
        h.desc.setText(descs[position]);
    }

    @Override public int getItemCount() { return titulos.length; }

    static class PageVH extends RecyclerView.ViewHolder {
        final ImageView img; final TextView titulo; final TextView desc;
        PageVH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgOnb);
            titulo = v.findViewById(R.id.txtTitulo);
            desc = v.findViewById(R.id.txtDesc);
        }
    }
}
