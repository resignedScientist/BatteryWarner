package com.laudien.p1xelfehler.batterywarner.Preferences.InfoNotificationActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

public class InfoNotificationFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.info_notification_items);

        Context context = getActivity();
        if (context != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            if (!dischargingServiceEnabled) {
                String summary = String.format(Locale.getDefault(), "'%s' %s", getString(R.string.title_discharging_service_enabled), getString(R.string.summary_dependency));
                Preference pref_screenOn = findPreference(getString(R.string.pref_info_screen_on));
                pref_screenOn.setEnabled(false);
                pref_screenOn.setSummary(summary);
                Preference pref_screenOff = findPreference(getString(R.string.pref_info_screen_off));
                pref_screenOff.setEnabled(false);
                pref_screenOff.setSummary(summary);
            }
        }
    }
}