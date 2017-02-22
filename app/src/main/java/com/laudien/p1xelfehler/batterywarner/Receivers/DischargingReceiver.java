package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Activities.MainActivity.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;

import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.NOTIFICATION_ID_STOP_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.NOTIFICATION_ID_WARNING_HIGH;

public class DischargingReceiver extends BroadcastReceiver {
    //private static final String TAG = "DischargingReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default)))
            return; // return if intro was not finished

        // add a delay for the dismissing of the notification if stop charging is enabled
        int delay = 0;
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default))) {
            delay = 3000;
            // show the stop charging notification
            NotificationBuilder.showNotification(context, NOTIFICATION_ID_STOP_CHARGING);
        }

        // dismiss warning high notification
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(NOTIFICATION_ID_WARNING_HIGH);
            }
        }, delay);

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

        // auto save if enabled in settings and last charging type (usb/ac/wireless) is enabled
        if (Contract.IS_PRO
                && BatteryAlarmManager.isChargingTypeEnabled(context, sharedPreferences)
                && sharedPreferences.getBoolean(context.getString(R.string.pref_graph_autosave), context.getResources().getBoolean(R.bool.pref_graph_autosave_default))) {
            GraphFragment.saveGraph(context);
        }
    }
}
