package com.example.laudien.batterywarner.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.laudien.batterywarner.R;
import com.example.laudien.batterywarner.Receiver.BatteryAlarmReceiver;

import static android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI;
import static com.example.laudien.batterywarner.Contract.DEF_WARNING_HIGH;
import static com.example.laudien.batterywarner.Contract.DEF_WARNING_LOW;
import static com.example.laudien.batterywarner.Contract.PICK_SOUND_REQUEST;
import static com.example.laudien.batterywarner.Contract.PREF_AC_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_IS_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_SOUND_URI;
import static com.example.laudien.batterywarner.Contract.PREF_USB_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_HIGH;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_HIGH_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_LOW;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_LOW_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_WIRELESS_ENABLED;
import static com.example.laudien.batterywarner.Contract.SHARED_PREFS;
import static com.example.laudien.batterywarner.Contract.WARNING_HIGH_MIN;
import static com.example.laudien.batterywarner.Contract.WARNING_LOW_MAX;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private static final String TAG = "SettingsActivity";
    private SharedPreferences sharedPreferences;
    private CheckBox checkBox_usb, checkBox_ac, checkBox_wireless, checkBox_lowBattery, checkBox_highBattery;
    private SeekBar seekBar_lowBattery, seekBar_highBattery;
    private TextView textView_lowBattery, textView_highBattery;
    Uri sound;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        textView_lowBattery = (TextView) findViewById(R.id.textView_lowBattery);
        textView_highBattery = (TextView) findViewById(R.id.textView_highBattery);
        seekBar_lowBattery = (SeekBar) findViewById(R.id.seekBar_lowBattery);
        seekBar_highBattery = (SeekBar) findViewById(R.id.seekBar_highBattery);
        checkBox_usb = (CheckBox) findViewById(R.id.checkBox_usb);
        checkBox_ac = (CheckBox) findViewById(R.id.checkBox_ac);
        checkBox_wireless = (CheckBox) findViewById(R.id.checkBox_wireless);
        checkBox_lowBattery = (CheckBox) findViewById(R.id.checkBox_lowBattery);
        checkBox_highBattery = (CheckBox) findViewById(R.id.checkBox_highBattery);

        checkBox_highBattery.setOnCheckedChangeListener(this);
        checkBox_highBattery.setChecked(sharedPreferences.getBoolean(PREF_WARNING_HIGH_ENABLED, true));
        seekBar_lowBattery.setOnSeekBarChangeListener(this);
        seekBar_highBattery.setOnSeekBarChangeListener(this);
        checkBox_usb.setOnCheckedChangeListener(this);
        checkBox_usb.setChecked(sharedPreferences.getBoolean(PREF_USB_ENABLED, true));
        checkBox_ac.setOnCheckedChangeListener(this);
        checkBox_ac.setChecked(sharedPreferences.getBoolean(PREF_AC_ENABLED, true));
        checkBox_wireless.setOnCheckedChangeListener(this);
        checkBox_wireless.setChecked(sharedPreferences.getBoolean(PREF_WIRELESS_ENABLED, true));
        checkBox_lowBattery.setOnCheckedChangeListener(this);
        checkBox_lowBattery.setChecked(sharedPreferences.getBoolean(PREF_WARNING_LOW_ENABLED, true));
        seekBar_lowBattery.setProgress(sharedPreferences.getInt(PREF_WARNING_LOW, DEF_WARNING_LOW));
        seekBar_highBattery.setProgress(sharedPreferences.getInt(PREF_WARNING_HIGH, DEF_WARNING_HIGH));

        textView_lowBattery.setText(getString(R.string.low_battery_warning) + " " + seekBar_lowBattery.getProgress() + "%");
        textView_highBattery.setText(getString(R.string.high_battery_warning) + " " + seekBar_highBattery.getProgress() + "%");

        Button btn_sound = (Button) findViewById(R.id.button_sound);
        btn_sound.setOnClickListener(this);

        // notification sound
        String uri = sharedPreferences.getString(PREF_SOUND_URI, "");
        if (!uri.equals(""))
            sound = Uri.parse(uri); // saved URI
        else // default URI
            sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
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
                        .putString(PREF_SOUND_URI, sound.toString())
                        .apply();

                // restart the alarm (if enabled)
                BatteryAlarmReceiver.cancelExistingAlarm(this);
                if (sharedPreferences.getBoolean(PREF_IS_ENABLED, true))
                    BatteryAlarmReceiver.setRepeatingAlarm(this, true);

                Log.i(TAG, getString(R.string.settings_saved));
                Toast.makeText(getApplicationContext(), getString(R.string.settings_saved), Toast.LENGTH_SHORT).show();
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
        int state = seekBar.getProgress();
        switch (seekBar.getId()) {
            case R.id.seekBar_lowBattery:
                if (state > WARNING_LOW_MAX)
                    seekBar.setProgress(WARNING_LOW_MAX);
                else
                    textView_lowBattery.setText(getString(R.string.low_battery_warning) + " " + state + "%");
                break;
            case R.id.seekBar_highBattery:
                if (state < WARNING_HIGH_MIN)
                    seekBar.setProgress(WARNING_HIGH_MIN);
                else
                    textView_highBattery.setText(getString(R.string.high_battery_warning) + " " + state + "%");
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_sound:
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.btn_notification) + ":");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, sound);
                startActivityForResult(intent, PICK_SOUND_REQUEST);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_OK) return;
        switch (requestCode){
            case PICK_SOUND_REQUEST: // notification sound picker
                sound = data.getParcelableExtra(EXTRA_RINGTONE_PICKED_URI);
                break;
        }
    }
}
