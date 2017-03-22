package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.laudien.p1xelfehler.batterywarner.Activities.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receivers.DischargingAlarmReceiver;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;
import com.laudien.p1xelfehler.batterywarner.Services.DischargingService;

import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.widget.Toast.LENGTH_SHORT;

public class OnOffButtonFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    private SharedPreferences sharedPreferences;
    private ToggleButton toggleButton;
    private BroadcastReceiver onOffChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            toggleButton.setOnCheckedChangeListener(null); // disable the toasts
            toggleButton.setChecked(sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default)));
            toggleButton.setOnCheckedChangeListener(OnOffButtonFragment.this); // enable the toasts
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        View view = inflater.inflate(R.layout.fragment_on_off_button, container, false);
        toggleButton = (ToggleButton) view.findViewById(R.id.toggleButton);
        boolean isChecked = sharedPreferences.getBoolean(getString(R.string.pref_is_enabled), getResources().getBoolean(R.bool.pref_is_enabled_default));
        toggleButton.setChecked(isChecked);
        toggleButton.setOnCheckedChangeListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(onOffChangedReceiver, new IntentFilter(Contract.BROADCAST_ON_OFF_CHANGED));
        onOffChangedReceiver.onReceive(getActivity(), null);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(onOffChangedReceiver);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Context context = getContext();
        Intent batteryStatus = getActivity().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            boolean isCharging = batteryStatus.getIntExtra(EXTRA_PLUGGED, -1) != 0;
            if (isChecked) { // turned on
                sharedPreferences.edit().putBoolean(getString(R.string.pref_already_notified), false).apply();
                if (isCharging) {
                    context.startService(new Intent(context, ChargingService.class));
                } else {
                    context.sendBroadcast(new Intent(Contract.BROADCAST_DISCHARGING_ALARM));
                    context.startService(new Intent(context, DischargingService.class));
                }
                ((BaseActivity) getActivity()).showToast(R.string.enabled_info, LENGTH_SHORT);
            } else {
                if (!isCharging) { // turned off and discharging
                    DischargingAlarmReceiver.cancelDischargingAlarm(context);
                }
                ((BaseActivity) getActivity()).showToast(R.string.disabled_info, LENGTH_SHORT);
            }
            // send broadcast
            context.sendBroadcast(new Intent(Contract.BROADCAST_ON_OFF_CHANGED));
        }
    }
}
