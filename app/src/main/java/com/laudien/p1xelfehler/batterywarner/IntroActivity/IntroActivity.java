package com.laudien.p1xelfehler.batterywarner.IntroActivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receivers.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Calendar;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

import static com.laudien.p1xelfehler.batterywarner.Contract.PREF_FIRST_START;
import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class IntroActivity extends MaterialIntroActivity {

    // private static final String TAG = "IntroActivity";
    private PreferencesSlide preferencesSlide;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enableLastSlideAlphaExitTransition(true); // enable that nice transition at the end

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
        preferencesSlide.saveSettings();
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(PREF_FIRST_START, false).apply();
        if (Contract.IS_PRO) {
            Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryStatus != null) {
                // start the service if charging (if enabled in settings)
                if (batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0
                        && BatteryAlarmManager.isChargingModeEnabled(sharedPreferences, batteryStatus)) {
                    sharedPreferences.edit().putLong(Contract.PREF_GRAPH_TIME, Calendar.getInstance().getTimeInMillis())
                            .putInt(Contract.PREF_LAST_PERCENTAGE, -1)
                            .apply();
                    startService(new Intent(this, ChargingService.class));
                }
            }
        }
        Toast.makeText(getApplicationContext(), getString(R.string.intro_finish_toast), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
    }
}
