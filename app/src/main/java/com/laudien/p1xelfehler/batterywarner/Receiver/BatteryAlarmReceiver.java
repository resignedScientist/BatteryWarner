package com.laudien.p1xelfehler.batterywarner.Receiver;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;
import com.laudien.p1xelfehler.batterywarner.Fragments.SettingsFragment;
import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Calendar;

public class BatteryAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryBroadcast";
    private static final int NO_STATE = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;

        // battery level
        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, NO_STATE);
        Log.i(TAG, "batteryLevel: " + batteryLevel + "%");
        // is it charging?
        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE) != 0;
        Log.i(TAG, "Charging: " + isCharging);
        // shared prefs
        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);

        if (isCharging) { // charging
            // return if charging type is disabled in settings
            int chargingType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE);
            switch (chargingType) {
                case BatteryManager.BATTERY_PLUGGED_AC: // ac charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_AC_ENABLED, true)) return;
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB: // usb charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_USB_ENABLED, true)) return;
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS: // wireless charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_WIRELESS_ENABLED, true)) return;
                    break;
            }

            // charge curve database
            boolean curveEnabled = sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true);
            if (curveEnabled) {
                int percentage = sharedPreferences.getInt(Contract.PREF_LAST_PERCENTAGE, NO_STATE);
                int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, NO_STATE);
                long timeNow = Calendar.getInstance().getTimeInMillis();
                long graphTime = timeNow - sharedPreferences.getLong(Contract.PREF_GRAPH_TIME, timeNow);
                if (graphTime < 100) graphTime = 0;
                if (percentage != batteryLevel) {
                    percentage = batteryLevel;
                    // write in database
                    GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(context);
                    dbHelper.addValue(graphTime, percentage, temperature);
                    // save in sharedPreferences
                    sharedPreferences.edit().putInt(Contract.PREF_LAST_PERCENTAGE, percentage).apply();
                }
            }

            int warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);
            if ((curveEnabled && batteryLevel < 100) || (!curveEnabled && batteryLevel <= warningHigh)) // new alarm
                setAlarm(context);

            // notification if warning value is reached
            if (batteryLevel >= warningHigh) {
                showNotification(context, context.getString(R.string.warning_high) + " " + warningHigh + "%!");
            }
        } else { // discharging
            int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
            if (batteryLevel <= warningLow) {
                showNotification(context, context.getString(R.string.warning_low) + " " + warningLow + "%!");
            } else {
                setAlarm(context);
            }
        }
    }

    private static void showNotification(Context context, String contentText) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Contract.PREF_ALREADY_NOTIFIED, false)) return;
        Log.i(TAG, "Showing notification: " + contentText);
        int icon = batteryStatus.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, NO_STATE);
        if (icon == NO_STATE)
            icon = android.R.drawable.alert_light_frame;
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(icon)
                .setSound(SettingsFragment.getNotificationSound(context))
                .setVibrate(new long[]{0, 300, 300, 300})
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
        sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, true).apply();
    }

    public static void setAlarm(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(Contract.PREF_IS_ENABLED, true)) return;
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;

        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, NO_STATE);
        int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
        int interval;
        long time = SystemClock.elapsedRealtime();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent batteryIntent = new Intent(context, BatteryAlarmReceiver.class);
        PendingIntent pendingIntent;
        boolean isCharging = isCharging(context);

        if (isCharging) { // Charging
            if (!sharedPreferences.getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true)) return;
            if (sharedPreferences.getBoolean(Contract.PREF_FASTER_INTERVAL, false))
                interval = Contract.INTERVAL_FAST_CHARGING;
            else
                interval = Contract.INTERVAL_CHARGING;
        } else { // Discharging
            if (!sharedPreferences.getBoolean(Contract.PREF_WARNING_LOW_ENABLED, true)) return;
            if (batteryLevel <= warningLow) {
                showNotification(context, context.getString(R.string.warning_low) + " " + warningLow + "%!");
                return;
            } else if (batteryLevel <= warningLow + 5)
                interval = Contract.INTERVAL_DISCHARGING_VERY_SHORT;
            else if (batteryLevel <= warningLow + 10)
                interval = Contract.INTERVAL_DISCHARGING_SHORT;
            else if (batteryLevel <= warningLow + 20)
                interval = Contract.INTERVAL_DISCHARGING_LONG;
            else
                interval = Contract.INTERVAL_DISCHARGING_VERY_LONG;
        }
        time += interval;
        pendingIntent = PendingIntent.getBroadcast(context, (int) time, batteryIntent, 0);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, pendingIntent);
        sharedPreferences.edit().putLong(Contract.PREF_INTENT_TIME, time).apply();
        Log.i(TAG, "Repeating alarm was set! interval = " + (double)interval / 60000 + " min");
    }

    public static void cancelExistingAlarm(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        long oldTime = sharedPreferences.getLong(Contract.PREF_INTENT_TIME, NO_STATE);

        if (oldTime != NO_STATE) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent oldBatteryIntent = new Intent(context, BatteryAlarmReceiver.class);
            PendingIntent oldPendingIntent = PendingIntent.getBroadcast(context,
                    (int) oldTime, oldBatteryIntent, 0);
            alarmManager.cancel(oldPendingIntent);
        }
        Log.i(TAG, "Repeating alarm was canceled!");
    }

    public static boolean isCharging(Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return batteryStatus != null && batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE) != 0;
    }
}
