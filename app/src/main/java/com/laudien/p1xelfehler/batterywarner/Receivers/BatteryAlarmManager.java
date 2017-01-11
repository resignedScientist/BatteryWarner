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

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;
import com.laudien.p1xelfehler.batterywarner.Fragments.SettingsFragment;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Calendar;

public class BatteryAlarmManager extends BroadcastReceiver {

    //private static final String TAG = "BatteryAlarmManager";
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
        //Log.i(TAG, "Repeating alarm was canceled!");
    }

    public static boolean isChargingModeEnabled(SharedPreferences sharedPreferences, Intent batteryStatus) {
        // returns true if the current charging mode is enabled

        if (!sharedPreferences.getBoolean(Contract.PREF_IS_ENABLED, true))
            return false; // return false if all warnings are disabled
        if (batteryStatus != null && batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED,
                Contract.NO_STATE) != 0) { // charging
            if (!sharedPreferences.getBoolean(Contract.PREF_WARNING_HIGH_ENABLED, true))
                return false; // return false if warning high is disabled
            // check charging type
            int chargingType = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE);
            switch (chargingType) {
                case android.os.BatteryManager.BATTERY_PLUGGED_AC: // ac charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_AC_ENABLED, true)) return false;
                    break;
                case android.os.BatteryManager.BATTERY_PLUGGED_USB: // usb charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_USB_ENABLED, true))
                        return false;
                    break;
                case android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS: // wireless charging
                    if (!sharedPreferences.getBoolean(Contract.PREF_WIRELESS_ENABLED, true))
                        return false;
                    break;
            }
        } else { // discharging
            if (!sharedPreferences.getBoolean(Contract.PREF_WARNING_LOW_ENABLED, true))
                return false; // return false if warning low is disabled
        }
        return true;
    }

    public void checkBattery(boolean logAndNotify) {
        batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;
        sharedPreferences = context.getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);

        if (!isChargingModeEnabled(sharedPreferences, batteryStatus))
            return; // return if current charging mode is disabled

        batteryLevel = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE);
        isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;

        // log messages
        /*Log.i(TAG, "Alarm received! (logAndNotify = " + logAndNotify + ")");
        Log.i(TAG, "batteryLevel: " + batteryLevel + "%");
        Log.i(TAG, "Charging: " + isCharging);*/

        // check battery and show notifications
        if (isCharging) { // charging
            int warningHigh = sharedPreferences.getInt(Contract.PREF_WARNING_HIGH, Contract.DEF_WARNING_HIGH);
            boolean graphEnabled = sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true);

            // notification if warning value is reached
            if (batteryLevel >= warningHigh) {
                showNotification(context.getString(R.string.warning_high) + " " + warningHigh + "%!");
            }

            // log data in database and set alarm
            if (logAndNotify) {
                if (graphEnabled) {
                    int percentage = sharedPreferences.getInt(Contract.PREF_LAST_PERCENTAGE, Contract.NO_STATE);
                    int temperature = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, Contract.NO_STATE);
                    long timeNow = Calendar.getInstance().getTimeInMillis();
                    long graphTime = timeNow - sharedPreferences.getLong(Contract.PREF_GRAPH_TIME, timeNow);
                    if (graphTime < 100) graphTime = 0;
                    if (percentage != batteryLevel) {
                        percentage = batteryLevel;
                        GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(context);
                        // if the graph is marked to be resetted -> reset it!
                        if (sharedPreferences.getBoolean(Contract.PREF_RESET_GRAPH, false)) {
                            sharedPreferences.edit().putBoolean(Contract.PREF_RESET_GRAPH, false).apply();
                            dbHelper.resetTable();
                        }
                        // write in database
                        dbHelper.addValue(graphTime, percentage, temperature);
                        // save in sharedPreferences
                        sharedPreferences.edit().putInt(Contract.PREF_LAST_PERCENTAGE, percentage).apply();
                        // send broadcast (database changed)
                        Intent intent = new Intent();
                        intent.setAction(Contract.BROADCAST_STATUS_CHANGED);
                        context.sendBroadcast(intent);
                    }
                }

                if (batteryLevel == 100)
                    context.stopService(new Intent(context, ChargingService.class));
            }
        } else { // discharging
            int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
            if (batteryLevel <= warningLow) { // show notification
                showNotification(context.getString(R.string.warning_low) + " " + warningLow + "%!");
            } else if (logAndNotify) { // set alarm
                setAlarm();
            }
        }
    }

    private void showNotification(String contentText) {
        if (batteryStatus == null) return;
        if (sharedPreferences.getBoolean(Contract.PREF_ALREADY_NOTIFIED, false)) return;

        //Log.i(TAG, "Showing notification: " + contentText);
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
        notificationManager.notify(Contract.NOTIFICATION_ID_BATTERY_WARNING, builder.build());
        sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, true).apply();
    }

    private void setAlarm() {
        if (!sharedPreferences.getBoolean(Contract.PREF_IS_ENABLED, true) || batteryStatus == null)
            return;
        if (isCharging) return; // only set alarm if discharging

        long time = Calendar.getInstance().getTimeInMillis();
        int warningLow = sharedPreferences.getInt(Contract.PREF_WARNING_LOW, Contract.DEF_WARNING_LOW);
        int interval;

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

        time += interval;

        Intent batteryIntent = new Intent(context, BatteryAlarmManager.class);
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context, (int) time, batteryIntent, 0); // request code = time
        alarmManager.setExact(AlarmManager.RTC, time, pendingIntent);
        sharedPreferences.edit().putLong(Contract.PREF_INTENT_TIME, time).apply();

        //Log.i(TAG, "Repeating alarm was set! interval = " + (double) interval / 60000 + " min");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        checkBattery(true);
    }
}
