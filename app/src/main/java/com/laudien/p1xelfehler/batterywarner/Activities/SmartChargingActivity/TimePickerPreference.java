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
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.ALARM_SERVICE;
import static java.text.DateFormat.SHORT;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.getInstance;

public class TimePickerPreference extends DialogPreference {

    private final String TAG = getClass().getSimpleName();
    private TimePicker timePicker = null;
    private Date date = new Date(Calendar.getInstance().getTimeInMillis());

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
        long time = getPersistedLong(getDefaultDate().getTime());
        boolean useAlarmClockTime = getSharedPreferences().getBoolean(getContext().getString(R.string.pref_smart_charging_use_alarm_clock_time), getContext().getResources().getBoolean(R.bool.pref_smart_charging_use_alarm_clock_time_default));
        // TODO: why again? :D
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP || !useAlarmClockTime) {
            persistLong(time);
        }
        date = new Date(time);
        DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());
        setSummary(dateFormat.format(time));
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            long time = getPersistedLong(getDefaultDate().getTime());
            date = new Date(time);
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
            DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());
            String timeString = dateFormat.format(date);
            persistString(timeString);
            setSummary(timeString);
        }
    }

    @Override
    protected boolean persistLong(long value) {
        while (date.getTime() <= value){
            value += 1000*60*60*24;
        }
        return super.persistLong(value);
    }

    private Date getDefaultDate(){
        Date date;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            if (alarmClockInfo != null) {
                long triggerTime = alarmClockInfo.getTriggerTime();
                date = new Date(triggerTime);
            } else {
                date = getDefaultDateIfNoAlarmClockIsSet();
            }
        } else { // if KitKat or no alarm clock set
            date = getDefaultDateIfNoAlarmClockIsSet();
        }
        return date;
    }
/*private Date getDate(String timeString) {
        DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());
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
    }*/

    /*private String getDefaultTimeString() {
        Date date;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmClockInfo = alarmManager.getNextAlarmClock();
            if (alarmClockInfo != null) {
                long triggerTime = alarmClockInfo.getTriggerTime();
                date = new Date(triggerTime);
            } else {
                date = getDefaultDateIfNoAlarmClockIsSet();
            }
        } else {
            date = getDefaultDateIfNoAlarmClockIsSet();
        }
        DateFormat dateFormat = DateFormat.getTimeInstance(SHORT, Locale.getDefault());
        return dateFormat.format(date);
    }*/

    private Date getDefaultDateIfNoAlarmClockIsSet() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(HOUR_OF_DAY, 6);
        calendar.set(MINUTE, 0);
        return new Date(calendar.getTimeInMillis());
    }
}
