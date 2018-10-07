package com.laudien.p1xelfehler.batterywarner.preferences.infoNotificationActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;

public class InfoNotificationFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.info_notification_items);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerObservers();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterObservers();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Context context = getActivity();
        if (context != null) {
            ServiceHelper.restartService(context);
        }
    }

    private void registerObservers() {
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private void unregisterObservers() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
