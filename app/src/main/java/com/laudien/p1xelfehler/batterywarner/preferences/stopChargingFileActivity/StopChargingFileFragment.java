package com.laudien.p1xelfehler.batterywarner.preferences.stopChargingFileActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;

public class StopChargingFileFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    EditTextPreference mPickPref, mEnablePref, mDisablePref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.stop_charging_file);
        preparePreferences();
        Context context = getActivity();
        if (context != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            pref.setSummary(((EditTextPreference) pref).getText());
        } else if (key.equals(getString(R.string.pref_stop_charging_auto_pick))) {
            boolean enabled = !((TwoStatePreference) pref).isChecked();
            mPickPref.setEnabled(enabled);
            mEnablePref.setEnabled(enabled);
            mDisablePref.setEnabled(enabled);
        }
    }

    private void preparePreferences() {
        TwoStatePreference autoPickPref = (TwoStatePreference) findPreference(getString(R.string.pref_stop_charging_auto_pick));
        mPickPref = (EditTextPreference) findPreference(getString(R.string.pref_stop_charging_file));
        mEnablePref = (EditTextPreference) findPreference(getString(R.string.pref_stop_charging_enable_charging_text));
        mDisablePref = (EditTextPreference) findPreference(getString(R.string.pref_stop_charging_disable_charging_text));
        boolean enabled = !autoPickPref.isChecked();
        mPickPref.setSummary(mPickPref.getText());
        mPickPref.setEnabled(enabled);
        mEnablePref.setSummary(mEnablePref.getText());
        mEnablePref.setEnabled(enabled);
        mDisablePref.setSummary(mDisablePref.getText());
        mDisablePref.setEnabled(enabled);
    }
}
