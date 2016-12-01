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

public class BatteryAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryBroadcast";
    private static final int NO_STATE = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;

        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, NO_STATE);
        Log.i(TAG, "batteryLevel: " + batteryLevel + "%");

        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE) != 0;
        Log.i(TAG, "Charging: " + isCharging);

        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
        int warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);
        boolean graphEnabled = sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true); // ToDo: Change to false


        if (isCharging) {
            // charge curve database
            if (graphEnabled) {
                int percentage = sharedPreferences.getInt(Contract.PREF_LAST_PERCENTAGE, NO_STATE);
                int graphTime = sharedPreferences.getInt(Contract.PREF_GRAPH_TIME, 0);
                graphTime++;
                if (percentage != batteryLevel) {
                    percentage = batteryLevel;
                    // write in database
                    GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(context);
                    dbHelper.addValue(graphTime, percentage);
                    // save in sharedPreferences
                    sharedPreferences.edit().putInt(Contract.PREF_LAST_PERCENTAGE, percentage).apply();
                }
                sharedPreferences.edit().putInt(Contract.PREF_GRAPH_TIME, graphTime).apply();
            }
            // return if charging type is disabled in settings
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
            // notifications
            if (batteryLevel >= warningHigh) {
                showNotification(context, context.getString(R.string.warning_high) + " " + warningHigh + "%!");
            } else {
                setAlarm(context);
            }
        } else {
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
        cancelExistingAlarm(context);
    }

    public static void setAlarm(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(Contract.PREF_IS_ENABLED, true)) return;
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;

        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, NO_STATE);
        int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
        int warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);
        int interval;
        long time = SystemClock.elapsedRealtime();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent batteryIntent = new Intent(context, BatteryAlarmReceiver.class);
        PendingIntent pendingIntent;
        boolean isCharging = isCharging(context);

        if (isCharging) { // Charging
            if (!sharedPreferences.getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true)) return;
            if (batteryLevel >= warningHigh) {
                showNotification(context, context.getString(R.string.warning_high) + " " + warningHigh + "%!");
                return;
            }
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
        if (isCharging)
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, pendingIntent);
        else
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, time, pendingIntent);
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

    public static boolean isCharging(Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return batteryStatus != null && batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE) != 0;
    }
}
