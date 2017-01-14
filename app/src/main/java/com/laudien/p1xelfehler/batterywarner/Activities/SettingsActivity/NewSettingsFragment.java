package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Locale;

public class NewSettingsFragment extends PreferenceFragment {

    private static final String TAG = "NewSettingsFragment";
    SwitchPreference switch_graphEnabled;
    PreferenceCategory category_graph;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        if (!Contract.IS_PRO) {
            switch_graphEnabled = (SwitchPreference) findPreference(getString(R.string.pref_graph_enabled));
            category_graph = (PreferenceCategory) findPreference("stats");
            switch_graphEnabled.setEnabled(false);
            category_graph.setTitle(String.format(Locale.getDefault(),
                    "%s (%s)", getString(R.string.stats), getString(R.string.pro_only_short)));
        }
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
}
