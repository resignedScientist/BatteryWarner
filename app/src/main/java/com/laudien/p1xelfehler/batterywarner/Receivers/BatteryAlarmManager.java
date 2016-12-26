package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;
import com.laudien.p1xelfehler.batterywarner.Fragments.SettingsFragment;
import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Calendar;

public class BatteryAlarmManager extends BroadcastReceiver {
    private static final String TAG = "BatteryAlarmManager";
    private Context context;
    private SharedPreferences sharedPreferences;
    private boolean isCharging;
    private int batteryLevel;
    private Intent batteryStatus;
    private AlarmManager alarmManager;

    public BatteryAlarmManager() {
    }

    public BatteryAlarmManager(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void checkBattery(boolean logAndSetAlarm) {
        batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;

        sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE);
        isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;

        // log messages
        Log.i(TAG, "Alarm received! (logAndSetAlarm = " + logAndSetAlarm + ")");
        Log.i(TAG, "batteryLevel: " + batteryLevel + "%");
        Log.i(TAG, "Charging: " + isCharging);

        // check battery and show notifications
        if (isCharging) { // charging
            int warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);
            boolean curveEnabled = sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true);

            // return if charging type is disabled in settings
            int chargingType = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE);
            switch (chargingType) {
                case android.os.BatteryManager.BATTERY_PLUGGED_AC: // ac charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_AC_ENABLED, true)) return;
                    break;
                case android.os.BatteryManager.BATTERY_PLUGGED_USB: // usb charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_USB_ENABLED, true)) return;
                    break;
                case android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS: // wireless charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_WIRELESS_ENABLED, true)) return;
                    break;
            }

            // notification if warning value is reached
            if (batteryLevel >= warningHigh) {
                showNotification(context.getString(R.string.warning_high) + " " + warningHigh + "%!");
            }

            // log data in database and set alarm
            if (logAndSetAlarm) {
                if (curveEnabled) {
                    int percentage = sharedPreferences.getInt(Contract.PREF_LAST_PERCENTAGE, Contract.NO_STATE);
                    int temperature = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, Contract.NO_STATE);
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
                        // send broadcast (database changed)
                        Intent intent = new Intent();
                        intent.setAction(Contract.BROADCAST_DATABASE_CHANGED);
                        context.sendBroadcast(intent);
                    }
                }
                if ((curveEnabled && batteryLevel < 100) || (!curveEnabled && batteryLevel <= warningHigh)) // new alarm
                    setAlarm();
            }
        } else { // discharging
            int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
            if (batteryLevel <= warningLow) { // show notification
                showNotification(context.getString(R.string.warning_low) + " " + warningLow + "%!");
            } else if (logAndSetAlarm) { // set alarm
                setAlarm();
            }
        }
    }

    private void showNotification(String contentText) {
        if (batteryStatus == null) return;
        if (sharedPreferences.getBoolean(Contract.PREF_ALREADY_NOTIFIED, false)) return;

        Log.i(TAG, "Showing notification: " + contentText);
        int icon = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_ICON_SMALL, Contract.NO_STATE);
        if (icon == Contract.NO_STATE)
            icon = android.R.drawable.alert_light_frame;
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(icon)
                .setSound(SettingsFragment.getNotificationSound(context))
                .setVibrate(new long[]{0, 300, 300, 300})
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
        sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, true).apply();
    }

    private void setAlarm() {
        if (!sharedPreferences.getBoolean(Contract.PREF_IS_ENABLED, true) || batteryStatus == null)
            return;

        int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
        int interval;
        long time = SystemClock.elapsedRealtime();

        if (isCharging) { // Charging
            if (!sharedPreferences.getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true)) return;
            if (sharedPreferences.getBoolean(Contract.PREF_FASTER_INTERVAL, false))
                interval = Contract.INTERVAL_FAST_CHARGING;
            else
                interval = Contract.INTERVAL_CHARGING;
        } else { // Discharging
            if (!sharedPreferences.getBoolean(Contract.PREF_WARNING_LOW_ENABLED, true)) return;
            if (batteryLevel <= warningLow) {
                showNotification(context.getString(R.string.warning_low) + " " + warningLow + "%!");
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

        Intent batteryIntent = new Intent(context, BatteryAlarmManager.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) time, batteryIntent, 0);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, pendingIntent);
        sharedPreferences.edit().putLong(Contract.PREF_INTENT_TIME, time).apply();

        Log.i(TAG, "Repeating alarm was set! interval = " + (double) interval / 60000 + " min");
    }

    public static void cancelExistingAlarm(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        long oldTime = sharedPreferences.getLong(Contract.PREF_INTENT_TIME, Contract.NO_STATE);

        if (oldTime != Contract.NO_STATE) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent oldBatteryIntent = new Intent(context, BatteryAlarmManager.class);
            PendingIntent oldPendingIntent = PendingIntent.getBroadcast(context,
                    (int) oldTime, oldBatteryIntent, 0);
            alarmManager.cancel(oldPendingIntent);
        }
        Log.i(TAG, "Repeating alarm was canceled!");
    }

    public static boolean isCharging(Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return batteryStatus != null && batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        checkBattery(true);
    }
}
