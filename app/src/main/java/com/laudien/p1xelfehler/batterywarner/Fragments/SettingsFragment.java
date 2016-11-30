package com.laudien.p1xelfehler.batterywarner.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receiver.BatteryAlarmReceiver;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "SettingsFragment";
    private SharedPreferences sharedPreferences;
    private CheckBox checkBox_usb, checkBox_ac, checkBox_wireless, checkBox_lowBattery, checkBox_highBattery;
    private SeekBar seekBar_lowBattery, seekBar_highBattery;
    private TextView textView_lowBattery, textView_highBattery;
    public Uri sound;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        sharedPreferences = getContext().getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);

        textView_lowBattery = (TextView) view.findViewById(R.id.textView_lowBattery);
        textView_highBattery = (TextView) view.findViewById(R.id.textView_highBattery);
        seekBar_lowBattery = (SeekBar) view.findViewById(R.id.seekBar_lowBattery);
        seekBar_highBattery = (SeekBar) view.findViewById(R.id.seekBar_highBattery);
        checkBox_usb = (CheckBox) view.findViewById(R.id.checkBox_usb);
        checkBox_ac = (CheckBox) view.findViewById(R.id.checkBox_ac);
        checkBox_wireless = (CheckBox) view.findViewById(R.id.checkBox_wireless);
        checkBox_lowBattery = (CheckBox) view.findViewById(R.id.checkBox_lowBattery);
        checkBox_highBattery = (CheckBox) view.findViewById(R.id.checkBox_highBattery);

        checkBox_highBattery.setOnCheckedChangeListener(this);
        checkBox_highBattery.setChecked(sharedPreferences.getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true));
        seekBar_lowBattery.setOnSeekBarChangeListener(this);
        seekBar_highBattery.setOnSeekBarChangeListener(this);
        checkBox_usb.setOnCheckedChangeListener(this);
        checkBox_usb.setChecked(sharedPreferences.getBoolean(Contract.PREF_USB_ENABLED, true));
        checkBox_ac.setOnCheckedChangeListener(this);
        checkBox_ac.setChecked(sharedPreferences.getBoolean(Contract.PREF_AC_ENABLED, true));
        checkBox_wireless.setOnCheckedChangeListener(this);
        checkBox_wireless.setChecked(sharedPreferences.getBoolean(Contract.PREF_WIRELESS_ENABLED, true));
        checkBox_lowBattery.setOnCheckedChangeListener(this);
        checkBox_lowBattery.setChecked(sharedPreferences.getBoolean(Contract.PREF_WARNING_LOW_ENABLED, true));
        seekBar_lowBattery.setProgress(sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW));
        seekBar_highBattery.setProgress(sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH));

        textView_lowBattery.setText(getString(R.string.low_battery_warning) + " " + seekBar_lowBattery.getProgress() + "%");
        textView_highBattery.setText(getString(R.string.high_battery_warning) + " " + seekBar_highBattery.getProgress() + "%");

        Button btn_sound = (Button) view.findViewById(R.id.button_sound);
        btn_sound.setOnClickListener(this);

        // notification sound
        sound = getNotificationSound(getContext());

        return view;
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_sound:
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.btn_notification) + ":");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, sound);
                startActivityForResult(intent, Contract.PICK_SOUND_REQUEST);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        int state = seekBar.getProgress();
        switch (seekBar.getId()) {
            case R.id.seekBar_lowBattery:
                if (state > Contract.WARNING_LOW_MAX)
                    seekBar.setProgress(Contract.WARNING_LOW_MAX);
                else
                    textView_lowBattery.setText(getString(R.string.low_battery_warning) + " " + state + "%");
                break;
            case R.id.seekBar_highBattery:
                if (state < Contract.WARNING_HIGH_MIN)
                    seekBar.setProgress(Contract.WARNING_HIGH_MIN);
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
                logText = "Low Battery percentage changed to " + seekBar.getProgress() + "%";
                break;
            case R.id.seekBar_highBattery:
                logText = "High Battery percentage changed to " + seekBar.getProgress() + "%";
                break;
        }
        if (logText != null)
            Log.i(TAG, logText);
    }

    public static Uri getNotificationSound(Context context) {
        String uri = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE)
                .getString(Contract.PREF_SOUND_URI, "");
        if (!uri.equals(""))
            return Uri.parse(uri); // saved URI
        else // default URI
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    public void saveAll() {
        sharedPreferences.edit()
                .putBoolean(Contract.PREF_USB_ENABLED, checkBox_usb.isChecked())
                .putBoolean(Contract.PREF_AC_ENABLED, checkBox_ac.isChecked())
                .putBoolean(Contract.PREF_WIRELESS_ENABLED, checkBox_wireless.isChecked())
                .putBoolean(Contract.PREF_WARNING_LOW_ENABLED, checkBox_lowBattery.isChecked())
                .putBoolean(Contract.PREF_WARNING_HIGH_ENABLED, checkBox_highBattery.isChecked())
                .putInt(Contract.PREF_WARNING_LOW, seekBar_lowBattery.getProgress())
                .putInt(Contract.PREF_WARNING_HIGH, seekBar_highBattery.getProgress())
                .putString(Contract.PREF_SOUND_URI, sound.toString())
                .apply();

        // restart the alarm (if enabled)
        BatteryAlarmReceiver.cancelExistingAlarm(getContext());
        if (sharedPreferences.getBoolean(Contract.PREF_IS_ENABLED, true))
            BatteryAlarmReceiver.setAlarm(getContext());

        Log.i(TAG, getString(R.string.settings_saved));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        switch (requestCode) {
            case Contract.PICK_SOUND_REQUEST: // notification sound picker
                sound = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        }
    }
}
