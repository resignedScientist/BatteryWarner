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

import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.RootChecker;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.os.BatteryManager.BATTERY_PLUGGED_USB;
import static com.laudien.p1xelfehler.batterywarner.Contract.NO_STATE;
import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_NOT_ROOTED;

/**
 * A BroadcastReceiver that is called by the system if the device has been plugged in to charge.
 * It cancels the battery low notification of the app, cancels the DischargingAlarm and
 * starts the ChargingService depending on the user preferences.
 * Does not work until the intro was finished.
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
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return false;
        }
        final int chargingType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, NO_STATE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isCharging = chargingType != 0;
        final boolean usbDisabled = sharedPreferences.getBoolean(context.getString(R.string.pref_usb_charging_disabled), context.getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
        boolean stopChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        if ((isCharging && ChargingService.isChargingTypeEnabled(context, batteryStatus)) || usbDisabled) {
            if (usbDisabled && chargingType == BATTERY_PLUGGED_USB) { // usb charging - but disabled in settings
                // disable charging
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            RootChecker.disableCharging(context);
                        } catch (RootChecker.NotRootedException e) { // not rooted notification
                            e.printStackTrace();
                            NotificationBuilder.showNotification(context, ID_NOT_ROOTED);
                        }
                    }
                });
                return false; // stop the method here, do repeat after 10s (= false)
            } else if (stopChargingEnabled) { // if stop charging feature is enabled
                // check/ask for root
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (!RootChecker.isRootAvailable()) {
                            // show notification if root is not available (or the user did not see the asking for it)
                            NotificationBuilder.showNotification(context, ID_NOT_ROOTED);
                        }
                    }
                });
            }
            ChargingService.startService(context); // start the charging service
            // show a notification if silent/vibrate mode is enabled
            NotificationBuilder.showNotification(context, NotificationBuilder.ID_SILENT_MODE);
            return true;
        } else {
            return false;
        }
    }
}
