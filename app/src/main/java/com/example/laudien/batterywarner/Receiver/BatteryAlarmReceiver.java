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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.example.laudien.batterywarner.Activities.SettingsActivity;
import com.example.laudien.batterywarner.R;

import static android.app.AlarmManager.ELAPSED_REALTIME;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.os.BatteryManager.BATTERY_PLUGGED_AC;
import static android.os.BatteryManager.BATTERY_PLUGGED_USB;
import static android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS;
import static android.os.BatteryManager.BATTERY_STATUS_CHARGING;
import static android.os.BatteryManager.BATTERY_STATUS_FULL;
import static android.os.BatteryManager.EXTRA_LEVEL;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.os.BatteryManager.EXTRA_STATUS;
import static com.example.laudien.batterywarner.Contract.DEF_WARNING_HIGH;
import static com.example.laudien.batterywarner.Contract.DEF_WARNING_LOW;
import static com.example.laudien.batterywarner.Contract.INTERVAL_CHARGING;
import static com.example.laudien.batterywarner.Contract.INTERVAL_DISCHARGING_LONG;
import static com.example.laudien.batterywarner.Contract.INTERVAL_DISCHARGING_SHORT;
import static com.example.laudien.batterywarner.Contract.INTERVAL_DISCHARGING_VERY_LONG;
import static com.example.laudien.batterywarner.Contract.INTERVAL_DISCHARGING_VERY_SHORT;
import static com.example.laudien.batterywarner.Contract.PREF_AC_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_INTENT_TIME;
import static com.example.laudien.batterywarner.Contract.PREF_IS_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_SOUND_URI;
import static com.example.laudien.batterywarner.Contract.PREF_USB_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_HIGH;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_HIGH_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_LOW;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_LOW_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_WIRELESS_ENABLED;
import static com.example.laudien.batterywarner.Contract.SHARED_PREFS;

public class BatteryAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryBroadcast";
    private static final int NO_STATE = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent batteryStatus = getBatteryStatus(context);
        if (batteryStatus == null) return;

        int batteryLevel = batteryStatus.getIntExtra(EXTRA_LEVEL, NO_STATE);
        boolean isCharging = isCharging(context);
        Log.i(TAG, "batteryLevel: " + batteryLevel + "%");

        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        int warningLow = sharedPreferences.getInt(PREF_WARNING_LOW, DEF_WARNING_LOW);
        int warningHigh = sharedPreferences.getInt(PREF_WARNING_HIGH, DEF_WARNING_HIGH);

        if (isCharging) {
            int chargingType = batteryStatus.getIntExtra(EXTRA_PLUGGED, NO_STATE);
            switch (chargingType) {
                case BATTERY_PLUGGED_AC:
                    if (!sharedPreferences.getBoolean(PREF_AC_ENABLED, true)) return;
                    break;
                case BATTERY_PLUGGED_USB:
                    if (!sharedPreferences.getBoolean(PREF_USB_ENABLED, true)) return;
                    break;
                case BATTERY_PLUGGED_WIRELESS:
                    if (!sharedPreferences.getBoolean(PREF_WIRELESS_ENABLED, true)) return;
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
                .setSound(SettingsActivity.getNotificationSound(context))
                .setVibrate(new long[]{0, 300, 300, 300})
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
        cancelExistingAlarm(context);
    }

    public static void setRepeatingAlarm(Context context, boolean firstTimeIsNow) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(PREF_IS_ENABLED, true)) return;

        int batteryLevel = getBatteryStatus(context).getIntExtra(EXTRA_LEVEL, NO_STATE);
        int warningLow = sharedPreferences.getInt(PREF_WARNING_LOW, DEF_WARNING_LOW);
        int warningHigh = sharedPreferences.getInt(PREF_WARNING_HIGH, DEF_WARNING_HIGH);
        long interval;
        if (isCharging(context)) {
            if (!sharedPreferences.getBoolean(PREF_WARNING_HIGH_ENABLED, true)) return;
            if (batteryLevel >= warningHigh) {
                showNotification(context, context.getString(R.string.warning_high) + " " + warningHigh + "%!");
                return;
            }
            interval = INTERVAL_CHARGING;
        } else {
            if (!sharedPreferences.getBoolean(PREF_WARNING_LOW_ENABLED, true)) return;
            if (batteryLevel <= warningLow) {
                showNotification(context, context.getString(R.string.warning_low) + " " + warningLow + "%!");
                return;
            } else if (batteryLevel <= warningLow + 5)
                interval = INTERVAL_DISCHARGING_VERY_SHORT;
            else if (batteryLevel <= warningLow + 10)
                interval = INTERVAL_DISCHARGING_SHORT;
            else if (batteryLevel <= warningLow + 20)
                interval = INTERVAL_DISCHARGING_LONG;
            else
                interval = INTERVAL_DISCHARGING_VERY_LONG;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long time = SystemClock.elapsedRealtime();
        if (!firstTimeIsNow)
            time += interval;
        Intent batteryIntent = new Intent(context, BatteryAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) time, batteryIntent, 0);
        alarmManager.setRepeating(ELAPSED_REALTIME, time, interval, pendingIntent);
        sharedPreferences.edit().putLong(PREF_INTENT_TIME, time).apply();
        Log.i(TAG, "Repeating alarm was set! interval = " + interval/60000 + " min");
    }

    public static void cancelExistingAlarm(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        long oldTime = sharedPreferences.getLong(PREF_INTENT_TIME, NO_STATE);

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

        int status = batteryStatus.getIntExtra(EXTRA_STATUS, NO_STATE);
        return status == BATTERY_STATUS_CHARGING || status == BATTERY_STATUS_FULL;
    }
}
