package com.example.laudien.batterywarner.Activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.example.laudien.batterywarner.CustomSlides.PreferencesSlide;
import com.example.laudien.batterywarner.R;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

public class IntroActivity extends MaterialIntroActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableLastSlideAlphaExitTransition(true); // enable that nice transition at the end

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorIntro1)
                .buttonsColor(R.color.colorButtons)
                .image(R.drawable.battery_status_full)
                .title(getString(R.string.intro_1_title))
                .description(getString(R.string.intro_1_description))
                .build()
        );

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorIntro2)
                .buttonsColor(R.color.colorButtons)
                .image(R.drawable.batteries)
                .title("How does it do that?")
                .description("Manifacturers say that your battery has a longer life, if you do not let the battery too long at 100% or reach 0%!")
                .build()
        );

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorIntro3)
                .buttonsColor(R.color.colorButtons)
                .image(R.drawable.ic_done_white_48dp)
                .title("For the longest life, you have to load \"flat\"!")
                .description("That means keeping the battery level for example between 20-80% either than 0-100%!")
                .build()
        );

        addSlide(new PreferencesSlide());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // enable full screen:
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
}
