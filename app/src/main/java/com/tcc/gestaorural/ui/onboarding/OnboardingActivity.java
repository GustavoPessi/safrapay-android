package com.tcc.gestaorural.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.tcc.gestaorural.R;
import com.tcc.gestaorural.ui.auth.LoginActivity;
import com.tcc.gestaorural.util.SessionManager;

import android.widget.Button;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private Button btnAvancar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pager = findViewById(R.id.pager);
        btnAvancar = findViewById(R.id.btnAvancar);
        Button btnPular = findViewById(R.id.btnPular);
        TabLayout indicador = findViewById(R.id.indicador);

        final OnboardingAdapter adapter = new OnboardingAdapter();
        pager.setAdapter(adapter);
        new TabLayoutMediator(indicador, pager, (tab, pos) -> {}).attach();

        btnPular.setOnClickListener(v -> concluir());

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                boolean ultima = position == adapter.getItemCount() - 1;
                btnAvancar.setText(ultima ? R.string.onb_start : R.string.onb_next);
            }
        });

        btnAvancar.setOnClickListener(v -> {
            int atual = pager.getCurrentItem();
            if (atual < adapter.getItemCount() - 1) {
                pager.setCurrentItem(atual + 1, true);
            } else {
                concluir();
            }
        });
    }

    private void concluir() {
        new SessionManager(this).marcarOnboardingVisto();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
