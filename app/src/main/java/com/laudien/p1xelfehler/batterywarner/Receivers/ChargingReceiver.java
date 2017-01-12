package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
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

        BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(context);
        batteryAlarmManager.cancelDischargingAlarm(context); // cancel discharging alarm

        // check if the charging notification for the current method is enabled
        if (!BatteryAlarmManager.isChargingNotificationEnabled(context, sharedPreferences)) {
            return;
        }

        // reset already notified
        sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, false).apply();

        // reset graph values if graph is enabled
        if (sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true)) {
            sharedPreferences.edit().putLong(Contract.PREF_GRAPH_TIME, Calendar.getInstance().getTimeInMillis())
                    .putInt(Contract.PREF_LAST_PERCENTAGE, -1)
                    .putBoolean(Contract.PREF_RESET_GRAPH, true)
                    .apply();
        }

        // notify the graph fragment
        GraphFragment.notify(context);

        // notify if silent/vibrate mode
        new NotificationBuilder(context).showNotification(NotificationBuilder.NOTIFICATION_SILENT_MODE);

        // cancel warning notifications
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NotificationBuilder.NOTIFICATION_ID_BATTERY_WARNING);

        // start charging service
        context.startService(new Intent(context, ChargingService.class));
    }
}
