package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

public class NewSettingsFragment extends PreferenceFragment implements SliderPreference.OnCheckedChangeListener {

    private static final String TAG = "NewSettingsFragment";
    SliderPreference sliderPreference_high;
    SwitchPreference switch_ac, switch_usb, switch_wireless;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        sliderPreference_high = (SliderPreference) findPreference(getString(R.string.pref_warning_high_enabled));
        sliderPreference_high.setOnCheckedChangeListener(this);
        switch_ac = (SwitchPreference) findPreference(getString(R.string.pref_ac_enabled));
        switch_usb = (SwitchPreference) findPreference(getString(R.string.pref_usb_enabled));
        switch_wireless = (SwitchPreference) findPreference(getString(R.string.pref_wireless_enabled));
    }

    @Override
    public void onStop() {
        super.onStop();
        // restart discharging alarm and charging service
        Context context = getActivity();
        if (context != null) {
            BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(context);
            batteryAlarmManager.cancelDischargingAlarm(context);
            batteryAlarmManager.setDischargingAlarm(context);
            context.startService(new Intent(context, ChargingService.class));
        }
    }

    @Override
    public void onCheckedChanged(boolean changedTo) {
        //Log.i(TAG, "changedTo = " + changedTo);
    }
}
