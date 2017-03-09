package com.laudien.p1xelfehler.batterywarner.Activities.IntroActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;
import com.laudien.p1xelfehler.batterywarner.Services.DischargingService;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

/**
 * An Activity that shows the app intro. It shows a different intro for the pro and the free
 * version of the app.
 * After it finished, it starts either the ChargingService, DischargingService or triggers a
 * DischargingAlarm depending on the user settings and starts the MainActivity.
 */
public class IntroActivity extends MaterialIntroActivity {

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

        if (Contract.IS_PRO) { // pro version
            // first slide (thank you slide)
            ImageSlide imageSlide = new ImageSlide();
            Bundle bundle = new Bundle(2);
            bundle.putString(ImageSlide.BUNDLE_TITLE, getString(R.string.thank_you_pro_title));
            bundle.putString(ImageSlide.BUNDLE_DESCRIPTION, getString(R.string.thank_you_pro_subtitle));
            imageSlide.setArguments(bundle);
            addSlide(imageSlide);
        } else { // free version
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
        }
        // preference slide
        addSlide(new PreferencesSlide());
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableFullscreen();
    }

    @Override
    public void onFinish() {
        super.onFinish();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(getString(R.string.pref_first_start), false).apply();
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            // start the service if charging
            boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
            if (isCharging) {
                ChargingService.startService(this);
            } else { // start the DischargingAlarmReceiver if discharging
                sendBroadcast(new Intent(Contract.BROADCAST_DISCHARGING_ALARM));
                boolean serviceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
                if (serviceEnabled) {
                    startService(new Intent(this, DischargingService.class));
                }
            }
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
