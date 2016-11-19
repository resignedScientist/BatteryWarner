package com.example.laudien.batterywarner;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.example.laudien.batterywarner.Receiver.BatteryAlarmReceiver;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
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
    private SharedPreferences sharedPreferences;
    private CheckBox checkBox_usb, checkBox_ac, checkBox_wireless, checkBox_lowBattery, checkBox_highBattery;
    private EditText editText_lowBattery, editText_highBattery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        editText_lowBattery = (EditText) findViewById(R.id.editText_lowBattery);
        editText_highBattery = (EditText) findViewById(R.id.editText_highBattery);
        checkBox_usb = (CheckBox) findViewById(R.id.checkBox_usb);
        checkBox_ac = (CheckBox) findViewById(R.id.checkBox_ac);
        checkBox_wireless = (CheckBox) findViewById(R.id.checkBox_wireless);
        checkBox_lowBattery = (CheckBox) findViewById(R.id.checkBox_lowBattery);
        checkBox_highBattery = (CheckBox) findViewById(R.id.checkBox_highBattery);

        checkBox_highBattery.setOnCheckedChangeListener(this);
        checkBox_highBattery.setChecked(sharedPreferences.getBoolean(PREF_WARNING_HIGH_ENABLED, true));
        editText_lowBattery.setText(Integer.toString(sharedPreferences.getInt(PREF_WARNING_LOW, 20)));
        editText_highBattery.setText(Integer.toString(sharedPreferences.getInt(PREF_WARNING_HIGH, 80)));
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        switch (compoundButton.getId()) {
            case R.id.checkBox_usb:
                sharedPreferences.edit().putBoolean(PREF_USB_ENABLED, checked).apply();
                break;
            case R.id.checkBox_ac:
                sharedPreferences.edit().putBoolean(PREF_AC_ENABLED, checked).apply();
                break;
            case R.id.checkBox_wireless:
                sharedPreferences.edit().putBoolean(PREF_WIRELESS_ENABLED, checked).apply();
                break;
            case R.id.checkBox_lowBattery:
                sharedPreferences.edit().putBoolean(PREF_WARNING_LOW_ENABLED, checked).apply();
                editText_lowBattery.setEnabled(checked);
                break;
            case R.id.checkBox_highBattery:
                sharedPreferences.edit().putBoolean(PREF_WARNING_HIGH_ENABLED, checked).apply();
                checkBox_ac.setEnabled(checked);
                checkBox_usb.setEnabled(checked);
                checkBox_wireless.setEnabled(checked);
                checkBox_ac.setChecked(checked);
                checkBox_usb.setChecked(checked);
                checkBox_wireless.setChecked(checked);
                editText_highBattery.setEnabled(checked);
                break;
        }
        if (!checkBox_ac.isChecked() && !checkBox_usb.isChecked() && !checkBox_wireless.isChecked())
            checkBox_highBattery.setChecked(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.edit()
                .putInt(PREF_WARNING_LOW, Integer.parseInt(editText_lowBattery.getText().toString()))
                .putInt(PREF_WARNING_HIGH, Integer.parseInt(editText_highBattery.getText().toString()))
                .apply();

        BatteryAlarmReceiver.cancelExistingAlarm(this);
        if (sharedPreferences.getBoolean(PREF_IS_ENABLED, true))
            BatteryAlarmReceiver.setRepeatingAlarm(this, BatteryAlarmReceiver.isCharging(this));
    }
}
