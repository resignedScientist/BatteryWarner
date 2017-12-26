package com.laudien.p1xelfehler.batterywarner.appIntro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;
import android.view.Window;
import android.view.WindowManager;

import com.laudien.p1xelfehler.batterywarner.MainActivity;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * An Activity that shows the app intro.
 * After it finished, it starts the {@link com.laudien.p1xelfehler.batterywarner.services.BackgroundService}
 * and the {@link com.laudien.p1xelfehler.batterywarner.MainActivity}.
 */
public class IntroActivity extends MaterialIntroActivity implements EasyModeSlide.EaseModeSlideDelegate {

    private PreferencesSlide preferencesSlide;
    private EasyModeSlide easyModeSlide;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // first slide
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorIntro1)
                .buttonsColor(R.color.colorButtons)
                .image(R.drawable.battery_status_full_green_256dp)
                .title(getString(R.string.intro_slide_1_title))
                .description(getString(R.string.intro_slide_1_description))
                .build()
        );
        // second slide
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorIntro2)
                .buttonsColor(R.color.colorButtons)
                .image(R.drawable.ic_batteries_400dp)
                .title(getString(R.string.intro_slide_2_title))
                .description(getString(R.string.intro_slide_2_description))
                .build()
        );
        // third slide
        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorIntro3)
                .buttonsColor(R.color.colorButtons)
                .image(R.drawable.ic_done_white_320dp)
                .title(getString(R.string.intro_slide_3_title))
                .description(getString(R.string.intro_slide_3_description))
                .build()
        );
        // easy mode selection slide
        easyModeSlide = new EasyModeSlide();
        easyModeSlide.delegate = this;
        addSlide(easyModeSlide);
        // preference slide
        preferencesSlide = new PreferencesSlide();
        addSlide(preferencesSlide);
    }

    @Override
    public void onFinish() {
        super.onFinish();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit()
                .putBoolean(getString(R.string.pref_first_start), false)
                .putBoolean(getString(R.string.pref_easy_mode), easyModeSlide.easyMode)
                .apply();
        // start services
        ServiceHelper.startService(this);
        // send toast
        ToastHelper.sendToast(getApplicationContext(), R.string.intro_finish_toast, LENGTH_SHORT);
        // start MainActivity
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    public void onModeSelected(boolean easyMode) {
        if (preferencesSlide != null) {
            preferencesSlide.loadPreferences(easyMode);
        }
    }
}
