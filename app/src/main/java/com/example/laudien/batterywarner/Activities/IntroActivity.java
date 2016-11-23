package com.example.laudien.batterywarner.Activities;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.example.laudien.batterywarner.Contract;
import com.example.laudien.batterywarner.CustomSlides.PreferencesSlide;
import com.example.laudien.batterywarner.Fragments.SettingsFragment;
import com.example.laudien.batterywarner.R;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

public class IntroActivity extends MaterialIntroActivity {

    Uri sound;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sound = SettingsFragment.getNotificationSound(this);
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
                .description("Manufacturers say that your battery has a longer life, if you do not let the battery too long at 100% or reach 0%!")
                .build()
        );

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorIntro3)
                .buttonsColor(R.color.colorButtons)
                .image(R.drawable.done_white_big)
                .title("For the longest life, you have to charge \"flat\"!")
                .description("That means keeping the battery level for example between 20-80% instead of 0-100%!")
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

    @Override
    public void onFinish() {
        super.onFinish();
        Toast.makeText(getApplicationContext(), "Let's make your battery last longer!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        switch (requestCode) {
            case Contract.PICK_SOUND_REQUEST: // notification sound picker
                sound = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE).edit()
                        .putString(Contract.PREF_SOUND_URI, sound.toString())
                        .apply();
                break;
        }
    }
}
