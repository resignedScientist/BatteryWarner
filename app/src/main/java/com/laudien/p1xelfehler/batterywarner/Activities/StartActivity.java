package com.laudien.p1xelfehler.batterywarner.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.Activities.IntroActivity.IntroActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.BatteryData;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.R;

/**
 * "Middle man" activity that starts either the IntroActivity or the MainActivity.
 */
public class StartActivity extends BaseActivity {
    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstStart = sharedPreferences.getBoolean(getString(R.string.pref_first_start), getResources().getBoolean(R.bool.pref_first_start_default));
        if (firstStart) {
            startActivity(new Intent(this, IntroActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationHelper.showBatteryInfoNotification(this, new BatteryData(
                    "1", "2", "3", "4", "5", "6"
            ));
        }
        finish();
    }
}
