package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

public class SliderPreference extends Preference implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "SliderPreference";
    private static final int DEFAULT_MAX = 100;
    private static final int DEFAULT_MIN = 0;
    private static final boolean DEFAULT_CHECKED = false;
    private static final int DEFAULT_PROGRESS = 0;
    private String key_progress = "";
    private int max;
    private int min;
    private String unit = "";
    private boolean checked;
    private boolean defaultChecked;
    private boolean initialized = false;
    private int progress;
    private int defaultProgress;
    private CheckBox checkBox;
    private SeekBar seekBar;
    private TextView textView;

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
            unit = a.getString(R.styleable.SliderPreference_unit);
            if (unit == null) {
                unit = "";
            }
            key_progress = a.getString(R.styleable.SliderPreference_key_progress);
            if (key_progress == null) {
                key_progress = "";
            }
            max = a.getInt(R.styleable.SliderPreference_slider_max, DEFAULT_MAX);
            min = a.getInt(R.styleable.SliderPreference_slider_min, DEFAULT_MIN);
            defaultChecked = a.getBoolean(R.styleable.SliderPreference_default_checked, DEFAULT_CHECKED);
            defaultProgress = a.getInt(R.styleable.SliderPreference_default_progress, DEFAULT_PROGRESS);
            a.recycle();
        } else {
            min = DEFAULT_MIN;
            max = DEFAULT_MAX;
            defaultChecked = DEFAULT_CHECKED;
            defaultProgress = DEFAULT_PROGRESS;
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        if (restorePersistedValue) { // load saved values
            checked = getPersistedBoolean(defaultChecked);
            if (!key_progress.equals("")) { // load progress
                progress = PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getInt(key_progress, defaultProgress);
            }
            initialized = true;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        checkBox = (CheckBox) view.findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(this);
        textView = (TextView) view.findViewById(R.id.textView);
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(max - min);

        Log.i(TAG, "progress = " + progress);

        if (initialized) {
            checkBox.setChecked(checked);
            seekBar.setProgress(progress - min);
        } else {
            checkBox.setChecked(defaultChecked);
            seekBar.setProgress(defaultProgress - min);
        }
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        textView.setText(String.format(Locale.getDefault(), "%d%s", i + min, unit));
        if (!key_progress.equals("")) { // save the progress
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putInt(key_progress, i + min)
                    .apply();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
