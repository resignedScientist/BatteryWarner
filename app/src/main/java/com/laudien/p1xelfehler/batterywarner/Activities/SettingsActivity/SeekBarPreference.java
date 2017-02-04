package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.Preference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "SeekBarPreference";
    private static final int DEFAULT_MAX = 100;
    private static final int DEFAULT_MIN = 0;
    private static final int DEFAULT_PROGRESS = 0;
    private int max;
    private int min;
    private String unit = "";
    private boolean initialized = false;
    private int progress;
    private int defaultProgress;
    private TextView textView;
    private SeekBar seekBar;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SeekBarPreference(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.preference_seekbar);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
            unit = a.getString(R.styleable.SeekBarPreference_unit);
            if (unit == null) {
                unit = "";
            }
            max = a.getInt(R.styleable.SeekBarPreference_slider_max, DEFAULT_MAX);
            min = a.getInt(R.styleable.SeekBarPreference_slider_min, DEFAULT_MIN);
            defaultProgress = a.getInt(R.styleable.SeekBarPreference_default_progress, DEFAULT_PROGRESS);
            a.recycle();
        } else {
            min = DEFAULT_MIN;
            max = DEFAULT_MAX;
            defaultProgress = DEFAULT_PROGRESS;
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        if (restorePersistedValue) { // load saved values
            progress = getPersistedInt(defaultProgress);
            initialized = true;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        textView = (TextView) view.findViewById(R.id.textView);
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(max - min);

        if (initialized) {
            seekBar.setProgress(progress - min);
        } else {
            seekBar.setProgress(defaultProgress - min);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        textView.setText(String.format(Locale.getDefault(), "%d%s", i + min, unit));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        persistInt(getValue());
    }

    public int getValue() {
        if (seekBar == null) {
            return min;
        }
        return seekBar.getProgress() + min;
    }

    public void setValue(int progress) {
        if (seekBar != null) {
            seekBar.setProgress(progress - min);
        }
    }
}
