package com.example.laudien.batterywarner.Activities;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.example.laudien.batterywarner.CustomSlides.PreferencesSlide;
import com.example.laudien.batterywarner.R;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragment;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

import static com.example.laudien.batterywarner.Contract.PREF_FIRST_START;
import static com.example.laudien.batterywarner.Contract.SHARED_PREFS;

public class IntroActivity extends MaterialIntroActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableLastSlideAlphaExitTransition(true);

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

        addSlide(new PreferencesSlide());
    }
}
