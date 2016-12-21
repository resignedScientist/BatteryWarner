package com.laudien.p1xelfehler.batterywarner.CustomSlides;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.laudien.p1xelfehler.batterywarner.Fragments.OnOffFragment;
import com.laudien.p1xelfehler.batterywarner.R;

import agency.tango.materialintroscreen.SlideFragment;

public class ImageSlide extends SlideFragment {

    private ImageView imageView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        imageView = (ImageView) view.findViewById(agency.tango.materialintroscreen.R.id.image_slide);
        OnOffFragment.setImageColor(getContext().getResources().getColor(R.color.colorBatteryOk), imageView);
        return view;
    }
}
