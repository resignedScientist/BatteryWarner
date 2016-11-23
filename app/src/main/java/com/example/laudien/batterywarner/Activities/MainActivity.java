package com.example.laudien.batterywarner.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.laudien.batterywarner.R;
import com.example.laudien.batterywarner.Receiver.BatteryAlarmReceiver;

import static android.widget.Toast.LENGTH_SHORT;
import static com.example.laudien.batterywarner.Contract.PREF_FIRST_START;
import static com.example.laudien.batterywarner.Contract.PREF_IS_ENABLED;
import static com.example.laudien.batterywarner.Contract.SHARED_PREFS;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
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
        final ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        toggleButton.setChecked(isChecked);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Log.i(TAG, "User changed status to " + isChecked);
                sharedPreferences.edit().putBoolean(PREF_IS_ENABLED, isChecked).apply();
                BatteryAlarmReceiver.cancelExistingAlarm(getApplicationContext());
                if (isChecked) {
                    BatteryAlarmReceiver.setRepeatingAlarm(getApplicationContext(), true);
                    Toast.makeText(getApplicationContext(), getString(R.string.enabled_info), LENGTH_SHORT).show();
                } else
                    Toast.makeText(getApplicationContext(), getString(R.string.disabled_info), LENGTH_SHORT).show();
            }
        });
        //startActivity(new Intent(this, IntroActivity.class));

        if (firstStart) {
            BatteryAlarmReceiver.setRepeatingAlarm(this, false);
            sharedPreferences.edit().putBoolean(PREF_FIRST_START, false).apply();
            //startActivity(new Intent(this, IntroActivity.class));
        }
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
