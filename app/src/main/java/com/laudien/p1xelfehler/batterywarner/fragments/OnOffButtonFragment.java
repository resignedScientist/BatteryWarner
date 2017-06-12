package com.laudien.p1xelfehler.batterywarner.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;
import com.laudien.p1xelfehler.batterywarner.receivers.DischargingAlarmReceiver;

import static android.content.Context.MODE_PRIVATE;
import static android.widget.Toast.LENGTH_SHORT;

public class OnOffButtonFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;
    private ToggleButton toggleButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        View view = inflater.inflate(R.layout.fragment_on_off_button, container, false);
        toggleButton = view.findViewById(R.id.toggleButton);
        boolean isChecked = sharedPreferences.getBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default));
        toggleButton.setChecked(isChecked);
        toggleButton.setOnCheckedChangeListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Context context = getContext();
        sharedPreferences.edit().putBoolean(getString(R.string.pref_is_enabled), isChecked).apply();
        if (isChecked) { // turned on
            SharedPreferences temporaryPrefs = context.getSharedPreferences(getString(R.string.prefs_temporary), MODE_PRIVATE);
            temporaryPrefs.edit().putBoolean(getString(R.string.pref_already_notified), false).apply();
            // start services
            ServiceHelper.startService(context, sharedPreferences, ServiceHelper.ID_CHARGING);
            ServiceHelper.startService(context, sharedPreferences, ServiceHelper.ID_DISCHARGING);
            // show toast
            ToastHelper.sendToast(context, R.string.toast_successfully_enabled, LENGTH_SHORT);
        } else { // turned off
            DischargingAlarmReceiver.cancelDischargingAlarm(context);
            ToastHelper.sendToast(context, R.string.toast_successfully_disabled, LENGTH_SHORT);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_is_enabled))) {
            toggleButton.setChecked(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_is_enabled_default)));
        }
    }
}
