package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.laudien.p1xelfehler.batterywarner.R;

public class NewSettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.new_settings_fragment);
    }
}
