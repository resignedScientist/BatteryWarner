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

/**
 * A custom slide for the app intro that uses a filter on the battery image to make it green.
 * You can give arguments to change the texts under the image.
 */
public class ImageSlide extends SlideFragment {

    /**
     * Argument key for the title under the image.
     */
    public final static String BUNDLE_TITLE = "title";
    /**
     * Argument key for the description under the title.
     */
    public final static String BUNDLE_DESCRIPTION = "description";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(agency.tango.materialintroscreen.R.layout.fragment_slide, container, false);

        String title, description;
        Bundle arguments = getArguments();
        if (arguments != null) {
            title = arguments.getString(BUNDLE_TITLE, getString(R.string.intro_1_title));
            description = arguments.getString(BUNDLE_DESCRIPTION, getString(R.string.intro_1_description));
        } else {
            title = getString(R.string.intro_1_title);
            description = getString(R.string.intro_1_description);
        }

        TextView titleTextView = (TextView) view.findViewById(agency.tango.materialintroscreen.R.id.txt_title_slide);
        titleTextView.setText(title);
        TextView descriptionTextView = (TextView) view.findViewById(agency.tango.materialintroscreen.R.id.txt_description_slide);
        descriptionTextView.setText(description);
        ImageView imageView = (ImageView) view.findViewById(agency.tango.materialintroscreen.R.id.image_slide);
        imageView.setImageResource(R.drawable.battery_status_full_white);
        Drawable drawable = imageView.getDrawable();
        drawable.setColorFilter(getContext().getResources().getColor(R.color.colorBatteryOk),
                PorterDuff.Mode.MULTIPLY);
        imageView.setImageDrawable(drawable);
        imageView.setVisibility(View.VISIBLE);
        return view;
    }

    public int backgroundColor() {
        return R.color.colorIntro1;
    }

    public int buttonsColor() {
        return R.color.colorButtons;
    }

}
