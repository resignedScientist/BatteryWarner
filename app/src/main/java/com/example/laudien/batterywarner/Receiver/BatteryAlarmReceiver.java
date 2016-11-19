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
import static com.example.laudien.batterywarner.Contract.INTERVAL_CHARGING;
import static com.example.laudien.batterywarner.Contract.INTERVAL_DISCHARGING;
import static com.example.laudien.batterywarner.Contract.PREF_AC_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_INTENT_TIME;
import static com.example.laudien.batterywarner.Contract.PREF_USB_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_HIGH;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_HIGH_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_LOW;
import static com.example.laudien.batterywarner.Contract.PREF_WARNING_LOW_ENABLED;
import static com.example.laudien.batterywarner.Contract.PREF_WIRELESS_ENABLED;
import static com.example.laudien.batterywarner.Contract.SHARED_PREFS;
import static com.example.laudien.batterywarner.Contract.DEF_WARNING_HIGH;
import static com.example.laudien.batterywarner.Contract.DEF_WARNING_LOW;

public class BatteryAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "BatteryBroadcast";
    private static final int NO_STATE = -1;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentFilter);
        if (batteryStatus == null) return;

        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        int warningLow = sharedPreferences.getInt(PREF_WARNING_LOW, DEF_WARNING_LOW);
        int warningHigh = sharedPreferences.getInt(PREF_WARNING_HIGH, DEF_WARNING_HIGH);

        boolean isCharging = isCharging(context);
        int batteryLevel = batteryStatus.getIntExtra(EXTRA_LEVEL, NO_STATE);
        Log.i(TAG, "batteryLevel = " + batteryLevel);

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
                showNotification(context.getString(R.string.warning_high) + " " + warningHigh + "%!");
            }
        } else if (batteryLevel <= warningLow) {
            showNotification(context.getString(R.string.warning_low) + " " + warningLow + "%!");
        }
    }

    public static boolean isCharging(Context context) {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentFilter);
        if (batteryStatus == null) return false;
        int status = batteryStatus.getIntExtra(EXTRA_STATUS, NO_STATE);
        return status == BATTERY_STATUS_CHARGING || status == BATTERY_STATUS_FULL;
    }

    private void showNotification(String contentText) {
        Log.i(TAG, "Showing notification: " + contentText);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.alert_light_frame)
                .setSound(sound)
                .setVibrate(new long[]{0, 300, 300, 300})
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
        cancelExistingAlarm(context);
    }

    public static void setRepeatingAlarm(Context context, boolean charging) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        long interval;
        if (charging) {
            if (!sharedPreferences.getBoolean(PREF_WARNING_HIGH_ENABLED, true)) return;
            interval = INTERVAL_CHARGING;
        } else {
            if (!sharedPreferences.getBoolean(PREF_WARNING_LOW_ENABLED, true)) return;
            interval = INTERVAL_DISCHARGING;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long time = SystemClock.elapsedRealtime();
        Intent batteryIntent = new Intent(context, BatteryAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) time, batteryIntent, 0);
        alarmManager.setRepeating(ELAPSED_REALTIME, time, interval, pendingIntent);
        sharedPreferences.edit().putLong(PREF_INTENT_TIME, time).apply();
        Log.i(TAG, "Repeating alarm was set!");
        Log.i(TAG, "Charging = " + charging);
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
}
