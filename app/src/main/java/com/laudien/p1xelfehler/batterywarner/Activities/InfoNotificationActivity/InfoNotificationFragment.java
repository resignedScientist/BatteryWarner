package com.laudien.p1xelfehler.batterywarner.Activities.InfoNotificationActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

import static android.os.Build.VERSION_CODES.N;

@RequiresApi(api = N)
public class InfoNotificationFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.info_notification_items);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean dischargingServiceEnabled = sharedPreferences.getBoolean(getString(R.string.pref_discharging_service_enabled), getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        if (!dischargingServiceEnabled){
            String summary = String.format(Locale.getDefault(), "'%s' %s", getString(R.string.log_percent_per_hour), getString(R.string.has_to_be_enabled));
            Preference pref_screenOn = findPreference(getString(R.string.pref_info_screen_on));
            pref_screenOn.setEnabled(false);
            pref_screenOn.setSummary(summary);
            Preference pref_screenOff = findPreference(getString(R.string.pref_info_screen_off));
            pref_screenOff.setEnabled(false);
            pref_screenOff.setSummary(summary);
        }
    }
}
