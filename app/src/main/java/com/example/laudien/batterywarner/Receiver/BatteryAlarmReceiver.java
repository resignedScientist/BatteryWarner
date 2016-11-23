package com.example.laudien.batterywarner.Receiver;

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

import com.example.laudien.batterywarner.Activities.SettingsActivity;
import com.example.laudien.batterywarner.Contract;
import com.example.laudien.batterywarner.Fragments.SettingsFragment;
import com.example.laudien.batterywarner.R;

public class BatteryAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryBroadcast";
    private static final int NO_STATE = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent batteryStatus = getBatteryStatus(context);
        if (batteryStatus == null) return;

        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, NO_STATE);
        boolean isCharging = isCharging(context);
        Log.i(TAG, "batteryLevel: " + batteryLevel + "%");

        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
        int warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);

        if (isCharging) {
            int chargingType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE);
            switch (chargingType) {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    if (!sharedPreferences.getBoolean(Contract.PREF_AC_ENABLED, true)) return;
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    if (!sharedPreferences.getBoolean(Contract.PREF_USB_ENABLED, true)) return;
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    if (!sharedPreferences.getBoolean(Contract.PREF_WIRELESS_ENABLED, true)) return;
                    break;
            }
            if (batteryLevel >= warningHigh) {
                showNotification(context, context.getString(R.string.warning_high) + " " + warningHigh + "%!");
            }
        } else {
            if (batteryLevel <= warningLow) {
                showNotification(context, context.getString(R.string.warning_low) + " " + warningLow + "%!");
            } else if (batteryLevel <= warningLow + 20) {
                Log.i(TAG, "Changing battery check frequency to a higher rate...");
                cancelExistingAlarm(context);
                setRepeatingAlarm(context, false);
            }
        }
    }

    private static void showNotification(Context context, String contentText) {
        Log.i(TAG, "Showing notification: " + contentText);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.alert_light_frame)
                .setSound(SettingsFragment.getNotificationSound(context))
                .setVibrate(new long[]{0, 300, 300, 300})
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
        cancelExistingAlarm(context);
    }

    public static void setRepeatingAlarm(Context context, boolean firstTimeIsNow) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(Contract.PREF_IS_ENABLED, true)) return;

        int batteryLevel = getBatteryStatus(context).getIntExtra(BatteryManager.EXTRA_LEVEL, NO_STATE);
        int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
        int warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);
        long interval;
        if (isCharging(context)) {
            if (!sharedPreferences.getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true)) return;
            if (batteryLevel >= warningHigh) {
                showNotification(context, context.getString(R.string.warning_high) + " " + warningHigh + "%!");
                return;
            }
            interval = Contract.INTERVAL_CHARGING;
        } else {
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
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long time = SystemClock.elapsedRealtime();
        if (!firstTimeIsNow)
            time += interval;
        Intent batteryIntent = new Intent(context, BatteryAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) time, batteryIntent, 0);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, time, interval, pendingIntent);
        sharedPreferences.edit().putLong(Contract.PREF_INTENT_TIME, time).apply();
        Log.i(TAG, "Repeating alarm was set! interval = " + interval / 60000 + " min");
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

    public static Intent getBatteryStatus(Context context) {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        return context.registerReceiver(null, intentFilter);
    }

    public static boolean isCharging(Context context) {
        Intent batteryStatus = getBatteryStatus(context);
        if (batteryStatus == null) return false;

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, NO_STATE);
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
    }
}
