package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;
import com.laudien.p1xelfehler.batterywarner.Fragments.SettingsFragment;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Calendar;

import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class ChargingReceiver extends BroadcastReceiver {
    //private static final String TAG = "ChargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED")) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Contract.PREF_FIRST_START, true))
            return; // return if intro was not finished

        //Log.i(TAG, "User started charging!");

        BatteryAlarmManager.cancelExistingAlarm(context); // cancel alarm

        // send broadcast
        Intent databaseIntent = new Intent();
        databaseIntent.setAction(Contract.BROADCAST_STATUS_CHANGED);
        context.sendBroadcast(databaseIntent);

        // reset already notified
        sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, false).apply();

        // check for the correct charging method
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (!BatteryAlarmManager.isChargingModeEnabled(sharedPreferences, batteryStatus))
            return; // return if current charging type is disabled

        // reset graph values if graph is enabled
        if (sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true)) {
            GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(context);
            dbHelper.resetTable();
            sharedPreferences.edit().putLong(Contract.PREF_GRAPH_TIME, Calendar.getInstance().getTimeInMillis())
                    .putInt(Contract.PREF_LAST_PERCENTAGE, -1)
                    .apply();
        }

        // notify if silent/vibrate mode
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (
                !notificationManager.areNotificationsEnabled()
                        || ringerMode == AudioManager.RINGER_MODE_SILENT
                        || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            PendingIntent contentIntent = PendingIntent.getActivity(
                    context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder builder = new Notification.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setSound(SettingsFragment.getNotificationSound(context))
                    .setVibrate(new long[]{0, 300, 300, 300})
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.notifications_are_off))
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true);
            notificationManager.notify(Contract.NOTIFICATION_ID_SILENT_MODE, builder.build());
        }

        // start charging service
        context.startService(new Intent(context, ChargingService.class));
    }
}
