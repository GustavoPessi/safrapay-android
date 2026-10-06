package com.tcc.gestaorural.ui.equipe;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.tcc.gestaorural.R;
import com.tcc.gestaorural.util.SessionManager;

/** Liga o aviso "Você tem convite pra uma equipe" (layout view_banner_convite). */
public final class BannerConvite {

    private BannerConvite() {}

    public static void atualizar(Activity a) {
        View banner = a.findViewById(R.id.bannerConvite);
        if (banner == null) return;
        int n = new SessionManager(a).getConvitesPendentes();
        banner.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
        if (n <= 0) return;
        ((TextView) a.findViewById(R.id.txtBannerConvite)).setText(
                a.getResources().getQuantityString(R.plurals.equipe_banner_convites, n, n));
        a.findViewById(R.id.btnVerConvite).setOnClickListener(v ->
                a.startActivity(new Intent(a, ConvitesActivity.class)));
    }
}
