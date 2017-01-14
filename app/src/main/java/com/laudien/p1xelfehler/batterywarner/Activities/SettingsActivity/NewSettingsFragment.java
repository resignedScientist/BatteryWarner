package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Locale;

public class NewSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "NewSettingsFragment";
    SwitchPreference switch_ac, switch_usb, switch_wireless, switch_graphEnabled, switch_darkTheme;
    SliderPreference slider_warningHigh;
    PreferenceCategory category_graph;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        slider_warningHigh = (SliderPreference) findPreference(getString(R.string.pref_warning_high_enabled));
        slider_warningHigh.setOnPreferenceChangeListener(this);
        switch_ac = (SwitchPreference) findPreference(getString(R.string.pref_ac_enabled));
        switch_usb = (SwitchPreference) findPreference(getString(R.string.pref_usb_enabled));
        switch_wireless = (SwitchPreference) findPreference(getString(R.string.pref_wireless_enabled));
        switch_darkTheme = (SwitchPreference) findPreference(getString(R.string.pref_dark_theme_enabled));
        switch_darkTheme.setOnPreferenceChangeListener(this);

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

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == switch_darkTheme) {
            Context context = getActivity();
            if (context != null) {
                Toast.makeText(
                        context,
                        "Das Thema wird erst aktiviert, wenn die Settings geschlossen werden!",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
        return true;
    }
}
