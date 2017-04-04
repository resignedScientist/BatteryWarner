package com.laudien.p1xelfehler.batterywarner.Activities.IntroActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.HelperClasses.ImageHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import agency.tango.materialintroscreen.SlideFragment;

import static android.view.View.VISIBLE;
import static com.laudien.p1xelfehler.batterywarner.Contract.IS_PRO;

/**
 * A custom slide for the app intro with a battery image.
 * It uses a filter on the battery image to make it green
 * and different title and description for pro and free version of the app.
 */
public class BatterySlide extends SlideFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(agency.tango.materialintroscreen.R.layout.fragment_slide, container, false);
        TextView titleTextView = (TextView) view.findViewById(agency.tango.materialintroscreen.R.id.txt_title_slide);
        TextView descriptionTextView = (TextView) view.findViewById(agency.tango.materialintroscreen.R.id.txt_description_slide);
        // set title and description texts
        if (IS_PRO) { // pro version
            titleTextView.setText(R.string.thank_you_pro_title);
            descriptionTextView.setText(R.string.thank_you_pro_subtitle);
        } else { // free version
            titleTextView.setText(R.string.intro_1_title);
            descriptionTextView.setText(R.string.intro_1_description);
        }
        // image
        ImageView imageView = (ImageView) view.findViewById(agency.tango.materialintroscreen.R.id.image_slide);
        imageView.setImageResource(R.drawable.battery_status_full_white);
        ImageHelper.setImageColor(getContext().getResources().getColor(R.color.colorBatteryOk), imageView);
        imageView.setVisibility(VISIBLE);
        return view;
    }

    public int backgroundColor() {
        return R.color.colorIntro1;
    }

    public int buttonsColor() {
        return R.color.colorButtons;
    }

}
