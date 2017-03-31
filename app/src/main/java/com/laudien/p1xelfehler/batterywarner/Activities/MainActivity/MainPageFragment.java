package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.laudien.p1xelfehler.batterywarner.R;

/**
 * Fragment that shows some information about the current battery status. Refreshes automatically.
 * Contains a button for toggling all warnings or logging of the app.
 */
public class MainPageFragment extends Fragment {

    /**
     * Helper method for setting a color filter to an image.
     *
     * @param color     The color the filter should have.
     * @param imageView The ImageView the filter should be set to.
     */
    public static void setImageColor(int color, ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        imageView.setImageDrawable(drawable);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);

        return view;
    }
}
