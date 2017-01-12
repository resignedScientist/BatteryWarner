package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;

import java.util.Calendar;

public class BatteryAlarmManager {
    private static final String TAG = "BatteryAlarmManager";
    private static BatteryAlarmManager instance;
    private SharedPreferences sharedPreferences;
    private int batteryLevel, temperature, warningHigh, warningLow;
    private long graphTime; // time since beginning of charging

    private BatteryAlarmManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);
        warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
    }

    public static BatteryAlarmManager getInstance(Context context) {
        if (instance == null) {
            instance = new BatteryAlarmManager(context);
        }
        return instance;
    }

    public static boolean isChargingNotificationEnabled(Context context, SharedPreferences sharedPreferences) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return isChargingNotificationEnabled(sharedPreferences, batteryStatus);
    }

    public static boolean isChargingNotificationEnabled(SharedPreferences sharedPreferences, Intent batteryStatus) {
        // check if charging and notification of current charging method is enabled
        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
        if (!isCharging) {
            return false; // return false if not charging
        }
        if (!sharedPreferences.getBoolean(Contract.PREF_IS_ENABLED, true)) {
            return false; // return false if all warnings are disabled
        }
        int chargingType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE);
        switch (chargingType) {
            case BatteryManager.BATTERY_PLUGGED_AC: // ac charging
                if (!sharedPreferences.getBoolean(Contract.PREF_AC_ENABLED, true)) {
                    return false;
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_USB: // usb charging
                if (!sharedPreferences.getBoolean(Contract.PREF_USB_ENABLED, true)) {
                    return false;
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: // wireless charging
                if (!sharedPreferences.getBoolean(Contract.PREF_WIRELESS_ENABLED, true)) {
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
        if (isCharging) {
            return false; // return false if charging
        }
        if (!sharedPreferences.getBoolean(Contract.PREF_IS_ENABLED, true)) {
            return false; // return false if all warnings are disabled
        }
        return sharedPreferences.getBoolean(Contract.PREF_WARNING_LOW_ENABLED, true);
    }

    public void checkAndNotify(Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        checkAndNotify(context, batteryStatus);
    }

    public void checkAndNotify(Context context, Intent batteryStatus) {
        if (batteryStatus == null) {
            return;
        }
        sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Contract.PREF_ALREADY_NOTIFIED, false)) {
            return; // return if already notified
        }

        batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE);
        if (Contract.IS_PRO) {
            temperature = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, Contract.NO_STATE);
            long timeNow = Calendar.getInstance().getTimeInMillis();
            graphTime = timeNow - sharedPreferences.getLong(Contract.PREF_GRAPH_TIME, timeNow);
            if (graphTime < 100) graphTime = 0; // makes sure that every graph begins at 0 min
        }

        if (batteryLevel >= warningHigh) { // warning high
            new NotificationBuilder(context).showNotification(NotificationBuilder.NOTIFICATION_WARNING_HIGH);
        } else if (batteryLevel <= warningLow) { // warning low
            new NotificationBuilder(context).showNotification(NotificationBuilder.NOTIFICATION_WARNING_LOW);
        }
    }

    public void logInDatabase(Context context) {
        if (!Contract.IS_PRO) {
            return; // don't log if it's not the pro version
        }
        int lastPercentage = sharedPreferences.getInt(Contract.PREF_LAST_PERCENTAGE, Contract.NO_STATE);
        if (batteryLevel == lastPercentage) {
            return; // don't log if the battery level did not change
        }
        GraphDbHelper graphDbHelper = GraphDbHelper.getInstance(context);
        // if the graph is marked to be resetted -> reset it!
        if (sharedPreferences.getBoolean(Contract.PREF_RESET_GRAPH, false)) {
            sharedPreferences.edit().putBoolean(Contract.PREF_RESET_GRAPH, false).apply();
            graphDbHelper.resetTable();
        }
        graphDbHelper.addValue(graphTime, batteryLevel, temperature);
        sharedPreferences.edit().putInt(Contract.PREF_LAST_PERCENTAGE, batteryLevel).apply();
        GraphFragment.notify(context);
        Log.i(TAG, "logged in database");
    }

    public void setDischargingAlarm(Context context) {
        if (!isDischargingNotificationEnabled(context, sharedPreferences)) {
            return; // return if disabled in settings
        }
        long currentTime = Calendar.getInstance().getTimeInMillis();
        int interval;
        if (batteryLevel <= warningLow + 5) {
            interval = Contract.INTERVAL_DISCHARGING_VERY_SHORT;
        } else if (batteryLevel <= warningLow + 10) {
            interval = Contract.INTERVAL_DISCHARGING_SHORT;
        } else if (batteryLevel <= warningLow + 20) {
            interval = Contract.INTERVAL_DISCHARGING_LONG;
        } else {
            interval = Contract.INTERVAL_DISCHARGING_VERY_LONG;
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
        sharedPreferences.edit().putLong(Contract.PREF_INTENT_TIME, currentTime + interval).apply();
    }

    public void cancelDischargingAlarm(Context context) {
        long intentTime = sharedPreferences.getLong(Contract.PREF_INTENT_TIME, Contract.NO_STATE);
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

    int getBatteryLevel() {
        return batteryLevel;
    }

    int getWarningLow() {
        return warningLow;
    }

    public void notifyWarningLowChanged(int warningLow) {
        this.warningLow = warningLow;
    }

    public void notifyWarningHighChanged(int warningHigh) {
        this.warningHigh = warningHigh;
    }
}
