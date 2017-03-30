package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.DischargingService;

import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_STOP_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_WARNING_HIGH;

/**
 * A BroadcastReceiver called by the system when the device stops to charge.
 * It is also called when the app stops the charging.
 * It dismisses the battery high notification of the app. If the user has the stop charge or the
 * disable usb charging feature enabled, it triggers a notification where the user can turn on the
 * charging again.
 * Also, it starts the DischargingService or triggers the DischargingAlarm depending
 * on the user preferences.
 */
public class DischargingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default)))
            return; // return if intro was not finished

        // add a delay for the dismissing of the notification if stop charging is enabled
        int delay = 0;
        int lastChargingType = sharedPreferences.getInt(context.getString(R.string.pref_last_chargingType), Contract.NO_STATE);
        boolean stopCharging = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        boolean usbDisabled = sharedPreferences.getBoolean(context.getString(R.string.pref_usb_charging_disabled), context.getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
        if (stopCharging || (usbDisabled && lastChargingType == BatteryManager.BATTERY_PLUGGED_USB)) {
            delay = 5000;
            // show the stop charging notification
            NotificationHelper.showNotification(context, ID_STOP_CHARGING);
        }

        // dismiss warning high notification
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationHelper.cancelNotification(context, ID_WARNING_HIGH);
            }
        }, delay);

        // reset already notified
        sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), false).apply();

        // start discharging service if enabled
        boolean serviceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
        if (serviceEnabled) {
            context.startService(new Intent(context, DischargingService.class));
        } else { // else start DischargingReceiver which notifies or sets alarm
            context.sendBroadcast(new Intent(Contract.BROADCAST_DISCHARGING_ALARM));
        }
    }
}
