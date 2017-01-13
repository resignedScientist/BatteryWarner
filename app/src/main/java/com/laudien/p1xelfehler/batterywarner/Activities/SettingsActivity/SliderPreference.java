package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.Preference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.laudien.p1xelfehler.batterywarner.R;

public class SliderPreference extends Preference implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "SliderPreference";
    private static final int DEFAULT_MAX = 100;
    private static final boolean DEFAULT_CHECKED = false;
    private int max;
    private boolean checked;
    private boolean defaultChecked;
    private boolean initialized = false;
    private CheckBox checkBox;
    private SeekBar seekBar;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SliderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public SliderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SliderPreference(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.preference_slider);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderPreference);
            max = a.getInt(R.styleable.SliderPreference_slider_max, DEFAULT_MAX);
            defaultChecked = a.getBoolean(R.styleable.SliderPreference_default_checked, DEFAULT_CHECKED);
            a.recycle();
        } else {
            max = DEFAULT_MAX;
            defaultChecked = DEFAULT_CHECKED;
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        if (restorePersistedValue) { // load saved values
            checked = getPersistedBoolean(defaultChecked);
            initialized = true;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        checkBox = (CheckBox) view.findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(this);
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);

        if (initialized) {
            checkBox.setChecked(checked);
        } else {
            checkBox.setChecked(defaultChecked);
        }
        seekBar.setMax(max);
    }

    @Override
    protected void onClick() {
        super.onClick();
        checkBox.setChecked(!checkBox.isChecked());
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        seekBar.setEnabled(b);
        persistBoolean(b);
    }

}
