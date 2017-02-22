package com.laudien.p1xelfehler.batterywarner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.Receivers.DischargingAlarmReceiver;

import java.util.Calendar;

public class BatteryAlarmManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    // private static final String TAG = "BatteryAlarmManager";
    private static BatteryAlarmManager instance;
    private final String PREF_LAST_PERCENTAGE, PREF_WARNING_HIGH, PREF_WARNING_LOW, PREF_GRAPH_ENABLED;
    private final int PREF_WARNING_HIGH_DEFAULT, PREF_WARNING_LOW_DEFAULT;
    private final boolean PREF_GRAPH_ENABLED_DEFAULT;
    private SharedPreferences sharedPreferences;
    private int batteryLevel, temperature, warningHigh, warningLow;
    private boolean graphEnabled;

    private BatteryAlarmManager(Context context) {
        PREF_LAST_PERCENTAGE = context.getString(R.string.pref_last_percentage);
        PREF_WARNING_HIGH = context.getString(R.string.pref_warning_high);
        PREF_WARNING_LOW = context.getString(R.string.pref_warning_low);
        PREF_WARNING_HIGH_DEFAULT = context.getResources().getInteger(R.integer.pref_warning_high_default);
        PREF_WARNING_LOW_DEFAULT = context.getResources().getInteger(R.integer.pref_warning_low_default);
        PREF_GRAPH_ENABLED = context.getString(R.string.pref_graph_enabled);
        PREF_GRAPH_ENABLED_DEFAULT = context.getResources().getBoolean(R.bool.pref_graph_enabled_default);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        warningHigh = sharedPreferences.getInt(context.getString(R.string.pref_warning_high), context.getResources().getInteger(R.integer.pref_warning_high_default));
        warningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default));
        graphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
    }

    public static BatteryAlarmManager getInstance(Context context) {
        if (instance == null) {
            instance = new BatteryAlarmManager(context);
        }
        return instance;
    }

    public static boolean isChargingNotificationEnabled(Context context, SharedPreferences sharedPreferences) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return isChargingNotificationEnabled(context, sharedPreferences, batteryStatus);
    }

    public static boolean isChargingNotificationEnabled(Context context, SharedPreferences sharedPreferences, Intent batteryStatus) {
        // check if charging and notification of current charging method is enabled
        boolean isCharging = isCharging(batteryStatus);
        if (!isCharging) {
            return false; // return false if not charging
        }
        if (!sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default))) {
            return false; // return false if all warnings are disabled
        }
        if (!sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default))) {
            return false; // return false if warning high is disabled
        }
        int chargingType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE);
        sharedPreferences.edit().putInt(context.getString(R.string.pref_last_chargingType), chargingType).apply();
        return checkChargingType(context, sharedPreferences, chargingType);
    }

    public static boolean checkChargingType(Context context, SharedPreferences sharedPreferences) {
        int chargingType = PreferenceManager.getDefaultSharedPreferences(context).getInt(
                context.getString(R.string.pref_last_chargingType),
                BatteryManager.BATTERY_PLUGGED_AC
        );
        return checkChargingType(context, sharedPreferences, chargingType);
    }

    private static boolean checkChargingType(Context context, SharedPreferences sharedPreferences, int chargingType) {
        switch (chargingType) {
            case BatteryManager.BATTERY_PLUGGED_AC: // ac charging
                if (!sharedPreferences.getBoolean(context.getString(R.string.pref_ac_enabled), context.getResources().getBoolean(R.bool.pref_ac_enabled_default))) {
                    return false;
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_USB: // usb charging
                if (!sharedPreferences.getBoolean(context.getString(R.string.pref_usb_enabled), context.getResources().getBoolean(R.bool.pref_usb_enabled_default))) {
                    return false;
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: // wireless charging
                if (!sharedPreferences.getBoolean(context.getString(R.string.pref_wireless_enabled), context.getResources().getBoolean(R.bool.pref_wireless_enabled_default))) {
                    return false;
                }
        }
        return true;
    }

    public static boolean isDischargingNotificationEnabled(Context context, SharedPreferences sharedPreferences) {
        // check if discharging and notification is enabled
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return false;
        }
        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
        return !isCharging
                && sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default))
                && sharedPreferences.getBoolean(context.getString(R.string.pref_warning_low_enabled), context.getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
    }

    public static boolean isCharging(Intent batteryStatus) {
        return batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
    }

    public static Intent getBatteryStatus(Context context) {
        return context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public boolean isEnabled(Context context) {
        return sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
    }

    public boolean isWarningHighEnabled(Context context) {
        return sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
    }

    public boolean isGraphEnabled() {
        return graphEnabled;
    }

    public void setGraphEnabled(boolean enabled) {
        graphEnabled = enabled;
    }

    public void checkAndNotify(Context context) {
        checkAndNotify(context, getBatteryStatus(context));
    }

    public void checkAndNotify(Context context, Intent batteryStatus) {
        if (batteryStatus == null) {
            return;
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE);
        if (Contract.IS_PRO) {
            temperature = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, Contract.NO_STATE);
        }

        if (batteryLevel >= warningHigh) { // warning high
            NotificationBuilder.showNotification(context, NotificationBuilder.NOTIFICATION_WARNING_HIGH);
        } else if (batteryLevel <= warningLow) { // warning low
            NotificationBuilder.showNotification(context, NotificationBuilder.NOTIFICATION_WARNING_LOW);
        }
    }

    public void logInDatabase(Context context) {
        if (!Contract.IS_PRO || !graphEnabled) {
            return; // don't log if it's not the pro version or disabled
        }
        int lastPercentage = sharedPreferences.getInt(PREF_LAST_PERCENTAGE, Contract.NO_STATE);
        if (batteryLevel == lastPercentage) {
            return; // don't log if the battery level did not change
        }
        GraphDbHelper graphDbHelper = GraphDbHelper.getInstance(context);
        long timeNow = Calendar.getInstance().getTimeInMillis();
        graphDbHelper.addValue(timeNow, batteryLevel, temperature);
        sharedPreferences.edit().putInt(PREF_LAST_PERCENTAGE, batteryLevel).apply();
        GraphFragment.notify(context);
    }

    public void setDischargingAlarm(Context context) {
        if (!isDischargingNotificationEnabled(context, sharedPreferences)) {
            return; // return if disabled in settings
        }
        long currentTime = Calendar.getInstance().getTimeInMillis();
        int interval;
        int warningLow = sharedPreferences.getInt(context.getString(R.string.pref_warning_low), context.getResources().getInteger(R.integer.pref_warning_low_default));
        if (batteryLevel <= warningLow + context.getResources().getInteger(R.integer.difference_very_short)) {
            interval = context.getResources().getInteger(R.integer.interval_very_short);
        } else if (batteryLevel <= warningLow + context.getResources().getInteger(R.integer.difference_short)) {
            interval = context.getResources().getInteger(R.integer.interval_short);
        } else if (batteryLevel <= warningLow + context.getResources().getInteger(R.integer.difference_long)) {
            interval = context.getResources().getInteger(R.integer.interval_long);
        } else {
            interval = context.getResources().getInteger(R.integer.interval_very_long);
        }
        Intent batteryIntent = new Intent(context, DischargingAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) currentTime + interval, // request code = alarm time
                batteryIntent,
                0
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(
                AlarmManager.RTC,
                currentTime + interval,
                pendingIntent
        );
        sharedPreferences.edit().putLong(context.getString(R.string.pref_intent_time), currentTime + interval).apply();
    }

    public void cancelDischargingAlarm(Context context) {
        long intentTime = sharedPreferences.getLong(context.getString(R.string.pref_intent_time), Contract.NO_STATE);
        if (intentTime == Contract.NO_STATE) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent batteryIntent = new Intent(context, BatteryAlarmManager.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) intentTime,
                batteryIntent,
                0
        );
        alarmManager.cancel(pendingIntent);
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preference) {
        if (preference.equals(PREF_WARNING_HIGH)) {
            warningHigh = sharedPreferences.getInt(preference, PREF_WARNING_HIGH_DEFAULT);
        } else if (preference.equals(PREF_WARNING_LOW)) {
            warningLow = sharedPreferences.getInt(preference, PREF_WARNING_LOW_DEFAULT);
        } else if (preference.equals(PREF_GRAPH_ENABLED)) {
            graphEnabled = sharedPreferences.getBoolean(preference, PREF_GRAPH_ENABLED_DEFAULT);
        }
    }
}
