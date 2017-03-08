package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.RootChecker;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_NOT_ROOTED;

/**
 * A BroadcastReceiver that is called by the system if the device has been plugged in to charge.
 * It cancels the battery low notification of the app, cancels the DischargingAlarm and
 * starts the ChargingService depending on the user preferences.
 */
public class ChargingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED")) return;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean firstStart = sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default));
        boolean isEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
        if (firstStart || !isEnabled) {
            return; // return if intro was not finished or main switch is turned off
        }

        DischargingAlarmReceiver.cancelDischargingAlarm(context); // cancel discharging alarm

        // cancel warning low notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NotificationBuilder.ID_WARNING_LOW);

        // reset already notified
        sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), false).apply();

        if (!startService(context)) {
            // if not enabled check again in 10s to make sure it is correct
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startService(context);
                }
            }, 10000);
        }
    }

    /**
     * Checks if enabled, asks for root, starts the service and shows the silent mode notification.
     *
     * @param context An instance of the Context class.
     * @return Returns true if the current charging type is enabled, false if not.
     */
    private boolean startService(final Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return false;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
        boolean usbDisabled = sharedPreferences.getBoolean(context.getString(R.string.pref_usb_charging_disabled), context.getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
        boolean stopChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        if ((isCharging && ChargingService.isChargingTypeEnabled(context, batteryStatus)) || usbDisabled) {
            if (usbDisabled || stopChargingEnabled) { // if any root feature is enabled
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (!RootChecker.isRootAvailable()) { // show notification if the app has no root anymore!
                            NotificationBuilder.showNotification(context, ID_NOT_ROOTED);
                        }
                    }
                });
            }
            ChargingService.startService(context);
            NotificationBuilder.showNotification(context, NotificationBuilder.ID_SILENT_MODE);
            return true;
        } else {
            return false;
        }
    }
}
