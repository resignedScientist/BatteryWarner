package com.laudien.p1xelfehler.batterywarner.Activities.IntroActivity;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import agency.tango.materialintroscreen.SlideFragment;

public class ImageSlide extends SlideFragment {

    TextView titleTextView, descriptionTextView;
    ImageView imageView;
    int backgroundColor = R.color.colorIntro1,
            buttonsColor = R.color.colorButtons;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(agency.tango.materialintroscreen.R.layout.fragment_slide, container, false);
        titleTextView = (TextView) view.findViewById(agency.tango.materialintroscreen.R.id.txt_title_slide);
        titleTextView.setText(getContext().getString(R.string.intro_1_title));
        descriptionTextView = (TextView) view.findViewById(agency.tango.materialintroscreen.R.id.txt_description_slide);
        descriptionTextView.setText(getContext().getString(R.string.intro_1_description));

        imageView = (ImageView) view.findViewById(agency.tango.materialintroscreen.R.id.image_slide);
        imageView.setImageResource(R.drawable.battery_status_full_white);
        Drawable drawable = imageView.getDrawable();
        drawable.setColorFilter(getContext().getResources().getColor(R.color.colorBatteryOk),
                PorterDuff.Mode.MULTIPLY);
        imageView.setImageDrawable(drawable);
        imageView.setVisibility(View.VISIBLE);
        return view;
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public int buttonsColor() {
        return buttonsColor;
    }

}
