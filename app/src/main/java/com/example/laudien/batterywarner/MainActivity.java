package com.example.laudien.batterywarner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.example.laudien.batterywarner.Receiver.BatteryAlarmReceiver;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String SHARED_PREFS = "BatteryWarner";
    public static final String PREF_FIRST_START = "FirstStart";
    public static final String PREF_INTENT_TIME = "PendingIntentTime";
    public static final String PREF_IS_ENABLED = "IsEnabled";
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        boolean firstStart = sharedPreferences.getBoolean(PREF_FIRST_START, true);
        boolean isChecked = sharedPreferences.getBoolean(PREF_IS_ENABLED, true);
        Button btn_settings = (Button) findViewById(R.id.btn_settings);
        btn_settings.setOnClickListener(this);
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        toggleButton.setChecked(isChecked);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Log.i(TAG, "User changed status to " + isChecked);
                sharedPreferences.edit().putBoolean(PREF_IS_ENABLED, isChecked).apply();
                BatteryAlarmReceiver.cancelExistingAlarm(getApplicationContext());
                if (isChecked) {
                    BatteryAlarmReceiver.setRepeatingAlarm(getApplicationContext(),
                            BatteryAlarmReceiver.isCharging(getApplicationContext()));
                }
            }
        });

        if (firstStart)
            sharedPreferences.edit().putBoolean(PREF_FIRST_START, false).apply();
        else
            BatteryAlarmReceiver.cancelExistingAlarm(this);
        BatteryAlarmReceiver.setRepeatingAlarm(this, BatteryAlarmReceiver.isCharging(this));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
    }
}
