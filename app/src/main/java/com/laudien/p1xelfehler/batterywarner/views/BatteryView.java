package com.laudien.p1xelfehler.batterywarner.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.laudien.p1xelfehler.batterywarner.R;

import static android.graphics.PorterDuff.Mode.MULTIPLY;

public class BatteryView extends android.support.v7.widget.AppCompatImageView {
    public BatteryView(Context context) {
        super(context);
        init(context, null);
    }

    public BatteryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BatteryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.BatteryView,
                    0, 0
            );
            int color = typedArray.getColor(R.styleable.BatteryView_color, -1);
            if (color != -1) {
                setColor(color);
            }
        }
    }

    public void setColor(int color) {
        Drawable drawable = getDrawable();
        drawable.setColorFilter(color, MULTIPLY);
        setImageDrawable(drawable);
    }
}
