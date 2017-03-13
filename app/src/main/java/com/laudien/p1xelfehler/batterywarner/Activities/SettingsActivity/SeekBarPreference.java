package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.Preference;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

/**
 * A Custom preference that shows a SeeKBar and a TextView with the number.
 * You can change the number with the SeekBar or by clicking on the TextView.
 * In the XML you can define attributes for the minimum and maximum values and for the
 * unit used next to the number.
 */
public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private static final int DEFAULT_MAX = 100;
    private static final int DEFAULT_MIN = 0;
    private static final int DEFAULT_PROGRESS = 0;
    private int max;
    private int min;
    private String unit = "";
    private int progress;
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

    // only called if a default value is given
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_PROGRESS);
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
            a.recycle();
        } else {
            min = DEFAULT_MIN;
            max = DEFAULT_MAX;
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        if (restorePersistedValue) { // load saved values
            progress = getPersistedInt(DEFAULT_PROGRESS);
        } else { // load default values
            progress = (int) defaultValue;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        textView = (TextView) view.findViewById(R.id.textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(progress - min);
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

    @Override
    protected boolean persistInt(int value) {
        progress = value;
        return super.persistInt(value);
    }

    private void showDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.dialog_number_picker, null);
        final NumberPicker numberPicker = (NumberPicker) view.findViewById(R.id.numberPicker);
        numberPicker.setMinValue(min);
        numberPicker.setMaxValue(max);
        numberPicker.setValue(getValue());
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        new AlertDialog.Builder(getContext())
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setValue(numberPicker.getValue());
                    }
                }).create().show();
    }

    private int getValue() {
        if (seekBar == null) {
            return min;
        }
        return seekBar.getProgress() + min;
    }

    private void setValue(int progress) {
        if (seekBar != null) {
            seekBar.setProgress(progress - min);
        }
    }
}
