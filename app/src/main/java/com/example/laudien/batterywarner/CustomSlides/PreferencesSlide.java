package com.example.laudien.batterywarner.CustomSlides;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.laudien.batterywarner.R;

import agency.tango.materialintroscreen.SlideFragment;

public class PreferencesSlide extends SlideFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.slide_preferences, container, false);

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
        return false;
    }

    @Override
    public String cantMoveFurtherErrorMessage() {
        return null;
    }
}
