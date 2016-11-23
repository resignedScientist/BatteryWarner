package com.example.laudien.batterywarner.CustomSlides;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.laudien.batterywarner.Fragments.SettingsFragment;
import com.example.laudien.batterywarner.R;

import agency.tango.materialintroscreen.SlideFragment;

public class PreferencesSlide extends SlideFragment {

    SettingsFragment settingsFragment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.slide_preferences, container, false);
        settingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(R.id.container_layout, settingsFragment).commit();
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

    @Override
    public void onPause() {
        super.onPause();
        settingsFragment.saveAll();
    }
}
