package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.util.Calendar;

public class ChargingReceiver extends BroadcastReceiver {
    //private static final String TAG = "ChargingReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED")) return;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), true))
            return; // return if intro was not finished

        BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(context);
        batteryAlarmManager.cancelDischargingAlarm(context); // cancel discharging alarm

        // cancel warning notifications
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NotificationBuilder.NOTIFICATION_ID_BATTERY_WARNING);

        // reset already notified
        sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), false).apply();

        // check if the charging notification for the current method is enabled
        if (!BatteryAlarmManager.checkChargingType(context, sharedPreferences)) {
            return;
        }

        // reset graph values if graph is enabled
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), true)) {
            sharedPreferences.edit().putLong(context.getString(R.string.pref_graph_time), Calendar.getInstance().getTimeInMillis())
                    .putInt(context.getString(R.string.pref_last_percentage), -1)
                    .putBoolean(context.getString(R.string.pref_reset_graph), true)
                    .apply();
        }

        // notify the graph fragment
        GraphFragment.notify(context);

        // notify if silent/vibrate mode
        new NotificationBuilder(context).showNotification(NotificationBuilder.NOTIFICATION_SILENT_MODE);

        // start charging service
        context.startService(new Intent(context, ChargingService.class));
    }
}
