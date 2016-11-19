package com.example.laudien.batterywarner;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.laudien.batterywarner.Receiver.BatteryAlarmReceiver;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener {
    public static final String SHARED_PREFS = "BatteryWarner";
    public static final String PREF_FIRST_START = "FirstStart";
    public static final String PREF_INTENT_TIME = "PendingIntentTime";
    public static final String PREF_IS_ENABLED = "IsEnabled";
    public static final String PREF_USB_ENABLED = "usbEnabled";
    public static final String PREF_AC_ENABLED = "acEnabled";
    public static final String PREF_WIRELESS_ENABLED = "wirelessEnabled";
    public static final String PREF_WARNING_LOW_ENABLED = "warningLowEnabled";
    public static final String PREF_WARNING_HIGH_ENABLED = "warningHighEnabled";
    public static final String PREF_WARNING_LOW = "warningLow";
    public static final String PREF_WARNING_HIGH = "warningHigh";
    private static final String TAG = "SettingsActivity";
    private static final int WARNING_HIGH_MIN = 60;
    private static final int WARNING_LOW_MAX = 40;
    private SharedPreferences sharedPreferences;
    private CheckBox checkBox_usb, checkBox_ac, checkBox_wireless, checkBox_lowBattery, checkBox_highBattery;
    private SeekBar seekBar_lowBattery, seekBar_highBattery;
    private TextView textView_lowBattery, textView_highBattery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        seekBar_lowBattery = (SeekBar) findViewById(R.id.seekBar_lowBattery);
        seekBar_highBattery = (SeekBar) findViewById(R.id.seekBar_highBattery);
        checkBox_usb = (CheckBox) findViewById(R.id.checkBox_usb);
        checkBox_ac = (CheckBox) findViewById(R.id.checkBox_ac);
        checkBox_wireless = (CheckBox) findViewById(R.id.checkBox_wireless);
        checkBox_lowBattery = (CheckBox) findViewById(R.id.checkBox_lowBattery);
        checkBox_highBattery = (CheckBox) findViewById(R.id.checkBox_highBattery);
        textView_lowBattery = (TextView) findViewById(R.id.textView_lowBattery);
        textView_highBattery = (TextView) findViewById(R.id.textView_highBattery);

        checkBox_highBattery.setOnCheckedChangeListener(this);
        checkBox_highBattery.setChecked(sharedPreferences.getBoolean(PREF_WARNING_HIGH_ENABLED, true));
        seekBar_lowBattery.setOnSeekBarChangeListener(this);
        seekBar_highBattery.setOnSeekBarChangeListener(this);
        seekBar_lowBattery.setProgress(sharedPreferences.getInt(PREF_WARNING_LOW, 20));
        seekBar_highBattery.setProgress(sharedPreferences.getInt(PREF_WARNING_HIGH, 80));
        checkBox_usb.setOnCheckedChangeListener(this);
        checkBox_usb.setChecked(sharedPreferences.getBoolean(PREF_USB_ENABLED, true));
        checkBox_ac.setOnCheckedChangeListener(this);
        checkBox_ac.setChecked(sharedPreferences.getBoolean(PREF_AC_ENABLED, true));
        checkBox_wireless.setOnCheckedChangeListener(this);
        checkBox_wireless.setChecked(sharedPreferences.getBoolean(PREF_WIRELESS_ENABLED, true));
        checkBox_lowBattery.setOnCheckedChangeListener(this);
        checkBox_lowBattery.setChecked(sharedPreferences.getBoolean(PREF_WARNING_LOW_ENABLED, true));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.settings));
        }
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:
                sharedPreferences.edit()
                        .putBoolean(PREF_USB_ENABLED, checkBox_usb.isChecked())
                        .putBoolean(PREF_AC_ENABLED, checkBox_ac.isChecked())
                        .putBoolean(PREF_WIRELESS_ENABLED, checkBox_wireless.isChecked())
                        .putBoolean(PREF_WARNING_LOW_ENABLED, checkBox_lowBattery.isChecked())
                        .putBoolean(PREF_WARNING_HIGH_ENABLED, checkBox_highBattery.isChecked())
                        .putInt(PREF_WARNING_LOW, seekBar_lowBattery.getProgress())
                        .putInt(PREF_WARNING_HIGH, seekBar_highBattery.getProgress())
                        .apply();

                // restart the alarm (if enabled)
                BatteryAlarmReceiver.cancelExistingAlarm(this);
                if (sharedPreferences.getBoolean(PREF_IS_ENABLED, true))
                    BatteryAlarmReceiver.setRepeatingAlarm(this, BatteryAlarmReceiver.isCharging(this));

                Log.i(TAG, "Settings saved!");
                Toast.makeText(getApplicationContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
                finish(); // close the settings
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        switch (compoundButton.getId()) {
            case R.id.checkBox_lowBattery:
                seekBar_lowBattery.setEnabled(checked);
                break;
            case R.id.checkBox_highBattery:
                checkBox_ac.setEnabled(checked);
                checkBox_usb.setEnabled(checked);
                checkBox_wireless.setEnabled(checked);
                checkBox_ac.setChecked(checked);
                checkBox_usb.setChecked(checked);
                checkBox_wireless.setChecked(checked);
                seekBar_highBattery.setEnabled(checked);
                break;
        }
        if (!checkBox_ac.isChecked() && !checkBox_usb.isChecked() && !checkBox_wireless.isChecked())
            checkBox_highBattery.setChecked(false);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int stateInt = seekBar.getProgress();
        String state = String.valueOf(stateInt);
        switch (seekBar.getId()) {
            case R.id.seekBar_lowBattery:
                if(stateInt > WARNING_LOW_MAX)
                    seekBar.setProgress(WARNING_LOW_MAX);
                else
                    textView_lowBattery.setText("Low Battery warning: " + state + "%");
                break;
            case R.id.seekBar_highBattery:
                if(stateInt < WARNING_HIGH_MIN)
                    seekBar.setProgress(WARNING_HIGH_MIN);
                else
                    textView_highBattery.setText("High Battery warning: " + state + "%");
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        String logText = null;
        switch (seekBar.getId()) {
            case R.id.seekBar_lowBattery:
                logText = "Low Battery percentage changed to " + seekBar.getProgress();
                break;
            case R.id.seekBar_highBattery:
                logText = "High Battery percentage changed to " + seekBar.getProgress();
                break;
        }
        if (logText != null)
            Log.i(TAG, logText);
    }
}
