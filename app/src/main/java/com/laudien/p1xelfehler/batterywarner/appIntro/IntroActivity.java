package com.laudien.p1xelfehler.batterywarner.appIntro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;
import android.view.Window;
import android.view.WindowManager;

import com.laudien.p1xelfehler.batterywarner.AppInfoHelper;
import com.laudien.p1xelfehler.batterywarner.MainActivity;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * An Activity that shows the app intro. It shows a different intro for the pro and the free
 * version of the app.
 * After it finished, it starts either the ChargingService, DischargingService or triggers a
 * DischargingAlarm depending on the user settings and starts the MainActivity.
 */
public class IntroActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addSlide(new BatterySlide()); // first slide
        if (!AppInfoHelper.isPro()) { // free version
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
                    .image(R.drawable.ic_done_white_320dp)
                    .title(getString(R.string.intro_slide_3_title))
                    .description(getString(R.string.intro_slide_3_description))
                    .build()
            );
        } else {
            // uninstall the free app if it is installed
            PackageManager packageManager = getPackageManager();
            try {
                packageManager.getPackageInfo(AppInfoHelper.PACKAGE_NAME_FREE, PackageManager.GET_ACTIVITIES);
                addSlide(new UninstallSlide());
            } catch (PackageManager.NameNotFoundException e) { // one of the apps is not installed
            }
        }
        // preference slide
        addSlide(new PreferencesSlide());
    }

    @Override
    public void onFinish() {
        super.onFinish();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(getString(R.string.pref_first_start), false).apply();
        // start services
        ServiceHelper.startService(this, sharedPreferences, ServiceHelper.ID_DISCHARGING);
        ServiceHelper.startService(this, sharedPreferences, ServiceHelper.ID_CHARGING);
        // send toast
        ToastHelper.sendToast(getApplicationContext(), R.string.intro_finish_toast, LENGTH_SHORT);
        // start MainActivity
        startActivity(new Intent(this, MainActivity.class));
    }
}
