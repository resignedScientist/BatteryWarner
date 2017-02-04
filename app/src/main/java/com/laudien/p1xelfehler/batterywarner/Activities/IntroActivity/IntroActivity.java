package com.laudien.p1xelfehler.batterywarner.Activities.IntroActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

public class IntroActivity extends MaterialIntroActivity {

    // private static final String TAG = "IntroActivity";
    private PreferencesSlide preferencesSlide;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enableLastSlideAlphaExitTransition(true); // enable that nice transition at the end
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // enable fullscreen if fullscreen was disabled (e.g. if a dialog opens)
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        enableFullscreen();
                    }
                });

        addSlide(new ImageSlide());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorIntro2)
                .buttonsColor(R.color.colorButtons)
                .image(R.drawable.batteries)
                .title(getString(R.string.slide_2_title))
                .description(getString(R.string.slide_2_description))
                .build()
        );

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorIntro3)
                .buttonsColor(R.color.colorButtons)
                .image(R.drawable.done_white_big)
                .title(getString(R.string.slide_3_title))
                .description(getString(R.string.slide_3_description))
                .build()
        );

        preferencesSlide = new PreferencesSlide();
        addSlide(preferencesSlide);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableFullscreen();
    }

    @Override
    public void onFinish() {
        super.onFinish();
        preferencesSlide.saveSettings();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(getString(R.string.pref_first_start), false).apply();
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(this);
        if (batteryStatus != null) {
            // start the service if charging (if enabled in settings)
            if (BatteryAlarmManager.isChargingNotificationEnabled(this, sharedPreferences, batteryStatus)) {
                ChargingService.startService(this);
                // set the alarm if discharging (if enabled in settings)
            } else if (BatteryAlarmManager.isDischargingNotificationEnabled(this, sharedPreferences)) {
                batteryAlarmManager.setDischargingAlarm(this);
            }
            // notify if enabled/necessary
            batteryAlarmManager.checkAndNotify(this, batteryStatus);
        }
        Toast.makeText(getApplicationContext(), getString(R.string.intro_finish_toast), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
    }

    private void enableFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
}
