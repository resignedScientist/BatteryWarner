package com.laudien.p1xelfehler.batterywarner.Activities.SmartChargingActivity;

import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static java.text.DateFormat.SHORT;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.getInstance;

public class TimePickerPreference extends DialogPreference {

    private final String TAG = getClass().getSimpleName();
    private TimePicker timePicker = null;
    private Date date;
    private DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TimePickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public TimePickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TimePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TimePickerPreference(Context context) {
        super(context);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        Log.d(TAG, "onSetInitialValue()");
        String timeString = getPersistedString(getDefaultTimeString());
        date = getDate(timeString);
        setSummary(timeString);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Log.d(TAG, "onBindView()");
    }

    @Override
    protected View onCreateDialogView() {
        timePicker = new TimePicker(getContext());
        Calendar calendar = getInstance();
        calendar.setTime(date);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setMinute(calendar.get(MINUTE));
            timePicker.setHour(calendar.get(HOUR_OF_DAY));
        } else {
            timePicker.setCurrentMinute(calendar.get(MINUTE));
            timePicker.setCurrentHour(calendar.get(HOUR_OF_DAY));
        }
        return timePicker;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            Calendar calendar = Calendar.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                calendar.set(HOUR_OF_DAY, timePicker.getHour());
                calendar.set(MINUTE, timePicker.getMinute());
            } else {
                calendar.set(HOUR_OF_DAY, timePicker.getCurrentHour());
                calendar.set(MINUTE, timePicker.getCurrentMinute());
            }
            date = new Date(calendar.getTimeInMillis());
            String timeString = getTimeString();
            Log.d(TAG, timeString);
            persistString(timeString);
            setSummary(timeString);
        }
    }

    private Date getDate(String timeString) {
        try {
            return dateFormat.parse(timeString);

        } catch (ParseException e) {
            e.printStackTrace();
            try {
                return dateFormat.parse(getDefaultTimeString());
            } catch (ParseException e1) {
                e1.printStackTrace();
                return null;
            }
        }
    }

    private String getTimeString() {
        return dateFormat.format(date);
    }

    private String getDefaultTimeString() {
        Date date = new Date(Calendar.getInstance().getTimeInMillis());
        return dateFormat.format(date);
    }
}
