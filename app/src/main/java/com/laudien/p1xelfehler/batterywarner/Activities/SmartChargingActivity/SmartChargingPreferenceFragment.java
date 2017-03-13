package com.laudien.p1xelfehler.batterywarner.Activities.SmartChargingActivity;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;

public class SmartChargingPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.smart_charging);
    }
}
