package com.laudien.p1xelfehler.batterywarner.appIntro;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.preferences.SettingsFragment;

import agency.tango.materialintroscreen.SlideFragment;

/**
 * A custom slide for the app intro that shows the {@link com.laudien.p1xelfehler.batterywarner.preferences.SettingsFragment}.
 */
public class PreferencesSlide extends SlideFragment {
    private SettingsFragment easyPreferences;
    private SettingsFragment normalPreferences;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        easyPreferences = new SettingsFragment();
        Bundle bundle = new Bundle(1);
        bundle.putBoolean(SettingsFragment.EXTRA_EASY_MODE, true);
        easyPreferences.setArguments(bundle);
        normalPreferences = new SettingsFragment();
        bundle = new Bundle(1);
        bundle.putBoolean(SettingsFragment.EXTRA_EASY_MODE, false);
        normalPreferences.setArguments(bundle);
        Activity activity = getActivity();
        if (activity != null) {
            activity.getFragmentManager().beginTransaction()
                    .add(R.id.container_layout, normalPreferences)
                    .add(R.id.container_layout, easyPreferences)
                    .hide(normalPreferences)
                    .commit();
        }
        return inflater.inflate(R.layout.slide_preferences, container, false);
    }

    @Override
    public int backgroundColor() {
        return R.color.colorPreferencesSlide;
    }

    @Override
    public int buttonsColor() {
        return R.color.colorButtons;
    }

    @Override
    public boolean canMoveFurther() {
        return true;
    }

    @Override
    public String cantMoveFurtherErrorMessage() {
        return null;
    }

    public void loadPreferences(boolean easyMode) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.getFragmentManager().beginTransaction()
                    .hide(easyMode ? normalPreferences : easyPreferences)
                    .show(easyMode ? easyPreferences : normalPreferences)
                    .commit();
        }
    }
}
