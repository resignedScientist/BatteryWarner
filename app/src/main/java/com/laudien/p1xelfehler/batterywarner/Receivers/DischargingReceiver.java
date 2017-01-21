package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;

public class DischargingReceiver extends BroadcastReceiver {
    //private static final String TAG = "DischargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), true))
            return; // return if intro was not finished

        // cancel warning notifications
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NotificationBuilder.NOTIFICATION_ID_BATTERY_WARNING);

        // reset already notified
        sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), false).apply();

        // only if discharging notification is enabled
        if (BatteryAlarmManager.isDischargingNotificationEnabled(context, sharedPreferences)) {

            // send notification if under lowWarning
            BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(context);
            batteryAlarmManager.checkAndNotify(context);
            batteryAlarmManager.setDischargingAlarm(context);

            // notify GraphFragment
            GraphFragment.notify(context);

            // start discharging alarm
            batteryAlarmManager.setDischargingAlarm(context);
        }

        // auto save if enabled in settings and last charging type is enabled
        if (Contract.IS_PRO
                && BatteryAlarmManager.checkChargingType(context, sharedPreferences)
                && sharedPreferences.getBoolean(context.getString(R.string.pref_graph_autosave), false)) {
            GraphFragment.saveGraph(context);
        }
    }
}
