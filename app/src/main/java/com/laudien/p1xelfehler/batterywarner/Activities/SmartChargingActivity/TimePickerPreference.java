package com.laudien.p1xelfehler.batterywarner.Activities.SmartChargingActivity;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import com.laudien.p1xelfehler.batterywarner.R;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.ALARM_SERVICE;
import static java.text.DateFormat.SHORT;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.getInstance;

public class TimePickerPreference extends DialogPreference {

    private TimePicker timePicker = null;
    private long time = Calendar.getInstance().getTimeInMillis();

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
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        time = getPersistedLong(getDefaultTime());
        boolean useAlarmClockTime = getSharedPreferences().getBoolean(getContext().getString(R.string.pref_smart_charging_use_alarm_clock_time), getContext().getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        // save time only if the alarm clock time is not used or cannot be used (because it is available only on 5.0+)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP || !useAlarmClockTime) {
            persistLong(time);
        }
        DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());
        setSummary(dateFormat.format(time));
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            time = getPersistedLong(getDefaultTime());
            DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());
            setSummary(dateFormat.format(time));
        }
        super.setEnabled(enabled);
    }

    @Override
    protected View onCreateDialogView() {
        timePicker = new TimePicker(getContext());
        timePicker.setIs24HourView(android.text.format.DateFormat.is24HourFormat(getContext()));
        Calendar calendar = getInstance();
        calendar.setTimeInMillis(time);
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
            time = calendar.getTimeInMillis();
            DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());
            String timeString = dateFormat.format(time);
            setSummary(timeString);
            persistLong(time);
        }
    }

    @Override
    protected boolean persistLong(long value) {
        while (value <= Calendar.getInstance().getTimeInMillis()){
            value += 1000*60*60*24;
        }
        return super.persistLong(value);
    }

    private long getDefaultTime(){
        long time;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            if (alarmClockInfo != null) {
                time = alarmClockInfo.getTriggerTime();
            } else {
                time = getDefaultTimeIfNoAlarmClockIsSet();
            }
        } else { // if KitKat or no alarm clock set
            time = getDefaultTimeIfNoAlarmClockIsSet();
        }
        return time;
    }

    private long getDefaultTimeIfNoAlarmClockIsSet() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(HOUR_OF_DAY, 6);
        calendar.set(MINUTE, 0);
        return calendar.getTimeInMillis();
    }
}
