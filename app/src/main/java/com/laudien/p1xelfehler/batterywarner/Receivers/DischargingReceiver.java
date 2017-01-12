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

import static com.laudien.p1xelfehler.batterywarner.Contract.SHARED_PREFS;

public class DischargingReceiver extends BroadcastReceiver {
    //private static final String TAG = "DischargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean(Contract.PREF_FIRST_START, true))
            return; // return if intro was not finished

        // cancel warning notifications
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NotificationBuilder.NOTIFICATION_ID_BATTERY_WARNING);

        if (!BatteryAlarmManager.isDischargingNotificationEnabled(context, sharedPreferences)) {
            return; // return if discharging notification is disabled
        }

        // set already shown to false
        sharedPreferences.edit().putBoolean(Contract.PREF_ALREADY_NOTIFIED, false).apply();

        // send notification if under lowWarning
        BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(context);
        batteryAlarmManager.checkAndNotify(context);
        batteryAlarmManager.setDischargingAlarm(context);

        // notify GraphFragment
        GraphFragment.notify(context);

        // stop charging service and start discharging alarm
        context.stopService(new Intent(context, ChargingService.class));
        batteryAlarmManager.setDischargingAlarm(context);
    }
}
