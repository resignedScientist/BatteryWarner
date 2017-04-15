package com.laudien.p1xelfehler.batterywarner.Activities.IntroActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import agency.tango.materialintroscreen.SlideFragment;

import static android.view.View.VISIBLE;

public class ImageSlide extends SlideFragment {

    public static final String KEY_TITLE = "title";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_BACKGROUND_COLOR = "backgroundColor";
    private int backgroundColor = R.color.colorIntro1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_slide, container, false);
        // read and set title/image from arguments
        Bundle arguments = getArguments();
        if (arguments != null) {
            if (arguments.containsKey(KEY_TITLE)) {
                TextView titleTextView = (TextView) view.findViewById(R.id.txt_title_slide);
                titleTextView.setText(arguments.getString(KEY_TITLE));
            }
            if (arguments.containsKey(KEY_DESCRIPTION)) {
                TextView descriptionTextView = (TextView) view.findViewById(R.id.txt_description_slide);
                descriptionTextView.setText(arguments.getString(KEY_DESCRIPTION));
            }
            if (arguments.containsKey(KEY_IMAGE)){
                ImageView imageView = (ImageView) view.findViewById(R.id.image_slide);
                imageView.setImageResource(arguments.getInt(KEY_IMAGE));
                imageView.setVisibility(VISIBLE);
            }
            if (arguments.containsKey(KEY_BACKGROUND_COLOR)){
                backgroundColor = arguments.getInt(KEY_BACKGROUND_COLOR);
            }
        }
        return view;
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public int buttonsColor() {
        return R.color.colorButtons;
    }

}
