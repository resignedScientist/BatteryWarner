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

import com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.HelperClasses.RootHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.os.BatteryManager.BATTERY_PLUGGED_USB;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_STOP_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_STOP_CHARGING_NOT_WORKING;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_WARNING_LOW;

/**
 * A BroadcastReceiver that is called by the system if the device has been plugged in to charge.
 * It cancels the battery low notification of the app, cancels the DischargingAlarm and
 * starts the ChargingService depending on the user preferences.
 * Does not work until the intro was finished.
 */
public class ChargingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED")) { // correct intent action
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean firstStart = sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default));
            boolean isEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
            if (!firstStart && isEnabled) { // if intro was finished and main switch is turned on
                DischargingAlarmReceiver.cancelDischargingAlarm(context); // cancel discharging alarm
                NotificationHelper.cancelNotification(context, ID_WARNING_LOW); // cancel warning low notification
                // reset already notified
                sharedPreferences.edit().putBoolean(context.getString(R.string.pref_already_notified), false).apply();
                if (!startService(context)) { // if current charging type is not enabled check again in 10s to make sure it is correct
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startService(context);
                        }
                    }, 10000);
                }
            }
        }
    }

    /**
     * Checks if enabled, asks for root, starts the service and shows the silent mode notification.
     *
     * @param context An instance of the Context class.
     * @return Returns true if the current charging type is enabled, false if not. False will trigger
     * a timer to double check in 10 seconds if everything is correct. This is done, because sometimes the
     * BatteryManager does not know instantly with which charging type the device is charging.
     */
    private boolean startService(final Context context) {
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return false;
        }
        int chargingType = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isCharging = chargingType != 0;
        final boolean usbDisabled = sharedPreferences.getBoolean(context.getString(R.string.pref_usb_charging_disabled), context.getResources().getBoolean(R.bool.pref_usb_charging_disabled_default));
        boolean stopChargingEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default));
        if (isCharging && (ChargingService.isChargingTypeEnabled(context, chargingType, sharedPreferences) || usbDisabled)) {
            if (usbDisabled && chargingType == BATTERY_PLUGGED_USB) { // usb charging - but disabled in settings
                // disable charging
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            RootHelper.disableCharging();
                            NotificationHelper.showNotification(context, ID_STOP_CHARGING);
                        } catch (RootHelper.NotRootedException e) { // not rooted notification
                            e.printStackTrace();
                            NotificationHelper.showNotification(context, ID_NOT_ROOTED);
                        } catch (RootHelper.NoBatteryFileFoundException e) {
                            NotificationHelper.showNotification(context, ID_STOP_CHARGING_NOT_WORKING);
                        }
                    }
                });
                return false; // stop the method here, do repeat after 10s (= false)
            } else if (stopChargingEnabled) { // if stop charging feature is enabled
                // check/ask for root
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (!RootHelper.isRootAvailable()) {
                            // show notification if root is not available (or the user did not see the asking for it)
                            NotificationHelper.showNotification(context, ID_NOT_ROOTED);
                        }
                    }
                });
            }
            context.startService(new Intent(context, ChargingService.class)); // start the charging service
            // show a notification if silent/vibrate mode is enabled
            NotificationHelper.showNotification(context, NotificationHelper.ID_SILENT_MODE);
            return true;
        } else {
            return false;
        }
    }
}
