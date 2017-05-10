package com.laudien.p1xelfehler.batterywarner.appIntro;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.AppInfoHelper;
import com.laudien.p1xelfehler.batterywarner.MainActivity;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.receivers.DischargingAlarmReceiver;
import com.laudien.p1xelfehler.batterywarner.services.BatteryInfoNotificationService;
import com.laudien.p1xelfehler.batterywarner.services.ChargingService;
import com.laudien.p1xelfehler.batterywarner.services.DischargingService;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.IS_PRO;

/**
 * An Activity that shows the app intro. It shows a different intro for the pro and the free
 * version of the app.
 * After it finished, it starts either the ChargingService, DischargingService or triggers a
 * DischargingAlarm depending on the user settings and starts the MainActivity.
 */
public class IntroActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        enableLastSlideAlphaExitTransition(true); // enable that nice transition at the end
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addSlide(new BatterySlide()); // first slide
        if (!IS_PRO) { // free version
            // second slide
            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.colorIntro2)
                    .buttonsColor(R.color.colorButtons)
                    .image(R.drawable.batteries)
                    .title(getString(R.string.intro_slide_2_title))
                    .description(getString(R.string.intro_slide_2_description))
                    .build()
            );
            // third slide
            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.colorIntro3)
                    .buttonsColor(R.color.colorButtons)
                    .image(R.drawable.done_white_big)
                    .title(getString(R.string.intro_slide_3_title))
                    .description(getString(R.string.intro_slide_3_description))
                    .build()
            );
        } else {
            // uninstall the free app if it is installed
            PackageManager packageManager = getPackageManager();
            try {
                packageManager.getPackageInfo(AppInfoHelper.PACKAGE_NAME_FREE, PackageManager.GET_ACTIVITIES);
                sendBroadcast(new Intent().setPackage(AppInfoHelper.PACKAGE_NAME_FREE).setClassName(AppInfoHelper.PACKAGE_NAME_FREE, "BothAppsInstalledReceiver"));
                addSlide(new UninstallSlide());
            } catch (PackageManager.NameNotFoundException e) { // one of the apps is not installed
            }
        }
        // preference slide
        addSlide(new PreferencesSlide());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onFinish() {
        super.onFinish();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(getString(R.string.pref_first_start), false).apply();
        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            // start the service if charging
            boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;
            if (isCharging) {
                startService(new Intent(this, ChargingService.class));
            } else { // start the DischargingAlarmReceiver if discharging
                sendBroadcast(new Intent(this, DischargingAlarmReceiver.class));
                boolean serviceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
                if (serviceEnabled) {
                    startService(new Intent(this, DischargingService.class));
                }
            }
        }
        startService(new Intent(this, BatteryInfoNotificationService.class));
        Toast.makeText(getApplicationContext(), getString(R.string.intro_finish_toast), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
    }
}
