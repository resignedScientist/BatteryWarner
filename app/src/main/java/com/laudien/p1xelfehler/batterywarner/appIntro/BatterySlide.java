package com.laudien.p1xelfehler.batterywarner.appIntro;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import agency.tango.materialintroscreen.SlideFragment;

/**
 * A custom slide for the app intro with a battery image.
 * It uses a filter on the battery image to make it green
 * and different title and description for pro and free version of the app.
 */
public class BatterySlide extends SlideFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.slide_battery, container, false);
        TextView titleTextView = view.findViewById(R.id.txt_title_slide);
        TextView descriptionTextView = view.findViewById(R.id.txt_description_slide);
        // set title and description texts
        titleTextView.setText(R.string.intro_slide_thank_you_title);
        descriptionTextView.setText(R.string.intro_slide_thank_you_description);
        return view;
    }

    public int backgroundColor() {
        return R.color.colorIntro1;
    }

    public int buttonsColor() {
        return R.color.colorButtons;
    }

}
