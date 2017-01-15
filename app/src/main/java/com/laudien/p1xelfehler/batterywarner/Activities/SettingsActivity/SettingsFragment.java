package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";
    private SwitchPreference switch_darkTheme;
    private RingtonePreference ringtonePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        SliderPreference slider_warningHigh = (SliderPreference) findPreference(getString(R.string.pref_warning_high_enabled));
        slider_warningHigh.setOnPreferenceChangeListener(this);
        switch_darkTheme = (SwitchPreference) findPreference(getString(R.string.pref_dark_theme_enabled));
        switch_darkTheme.setOnPreferenceChangeListener(this);
        ringtonePreference = (RingtonePreference) findPreference(getString(R.string.pref_sound_uri));
        ringtonePreference.setOnPreferenceChangeListener(this);

        Context context = getActivity();
        if (context != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String sound = sharedPreferences.getString(getString(R.string.pref_sound_uri), "");
            Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(sound));
            ringtonePreference.setSummary(ringtone.getTitle(context));
        }

        if (!Contract.IS_PRO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // lollipop and above
                SwitchPreference switch_graphEnabled = (SwitchPreference) findPreference(getString(R.string.pref_graph_enabled));
                switch_graphEnabled.setEnabled(false);
            } else { // kitkat
                CheckBoxPreference checkBox_graphEnabled = (CheckBoxPreference) findPreference(getString(R.string.pref_graph_enabled));
                checkBox_graphEnabled.setEnabled(false);
            }
            PreferenceCategory category_graph = (PreferenceCategory) findPreference("stats");
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
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(getString(R.string.pref_already_notified), false).apply();
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
                        getString(R.string.theme_activated_toast),
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else if (preference == ringtonePreference) {
            Context context = getActivity();
            if (context != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(o.toString()));
                ringtonePreference.setSummary(ringtone.getTitle(context));
            }
        }
        return true;
    }
}
