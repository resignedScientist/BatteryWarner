package com.laudien.p1xelfehler.batterywarner.Activities;

import android.content.Intent;
import android.content.SharedPreferences;

import com.laudien.p1xelfehler.batterywarner.Activities.IntroActivity.IntroActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Contract;

import static com.laudien.p1xelfehler.batterywarner.Contract.PREF_FIRST_START;

public class StartActivity extends BaseActivity {
    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences sharedPreferences = getSharedPreferences(Contract.SHARED_PREFS, MODE_PRIVATE);
        boolean firstStart = sharedPreferences.getBoolean(PREF_FIRST_START, true);
        if (firstStart) {
            startActivity(new Intent(this, IntroActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}
