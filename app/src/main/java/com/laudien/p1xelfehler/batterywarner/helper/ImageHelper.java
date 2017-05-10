package com.laudien.p1xelfehler.batterywarner.helper;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import static android.graphics.PorterDuff.Mode.MULTIPLY;

/**
 * Helper class for everything about images.
 */
public class ImageHelper {
    /**
     * Helper method for setting a color filter to an image.
     *
     * @param color     The color the filter should have.
     * @param imageView The ImageView the filter should be set to.
     */
    public static void setImageColor(int color, ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        drawable.setColorFilter(color, MULTIPLY);
        imageView.setImageDrawable(drawable);
    }
}
