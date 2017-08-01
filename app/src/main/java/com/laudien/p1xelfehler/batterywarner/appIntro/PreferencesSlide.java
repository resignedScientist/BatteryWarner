package com.laudien.p1xelfehler.batterywarner.appIntro;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.slide_preferences, container, false);
        getActivity().getFragmentManager().beginTransaction().replace(R.id.container_layout, new SettingsFragment()).commit();
        return view;
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
}
