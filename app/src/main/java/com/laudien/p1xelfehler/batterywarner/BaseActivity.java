package com.laudien.p1xelfehler.batterywarner;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import static com.laudien.p1xelfehler.batterywarner.Contract.PREF_DARK_THEME;
import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        if (sharedPreferences.getBoolean(PREF_DARK_THEME, false)) {
            setTheme(R.style.DarkTheme);
        }
    }
}
