package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.AppInfoHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.services.BatteryInfoNotificationService;
import com.laudien.p1xelfehler.batterywarner.services.ChargingService;
import com.laudien.p1xelfehler.batterywarner.services.DischargingService;

import static android.content.Context.MODE_PRIVATE;

/**
 * A BroadcastReceiver called by the System if the device finished booting.
 * It starts some services if necessary. Does only work after the intro was finished.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) { // correct intent action
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (!sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default))) { // intro was finished
                // start info notification service
                context.startService(new Intent(context, BatteryInfoNotificationService.class));
                Intent batteryStatus = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryStatus != null) { // given batteryStatus intent not null
                    // set already notified to false
                    SharedPreferences temporaryPrefs = context.getSharedPreferences(context.getString(R.string.prefs_temporary), MODE_PRIVATE);
                    temporaryPrefs.edit().putBoolean(context.getString(R.string.pref_already_notified), context.getResources().getBoolean(R.bool.pref_already_notified_default)).apply();
                    // start services/receivers
                    boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;
                    if (isCharging) { // charging
                        context.startService(new Intent(context, ChargingService.class)); // start charging service if enabled
                    } else { // discharging
                        boolean serviceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
                        if (serviceEnabled) { // discharging service enabled -> start it
                            context.startService(new Intent(context, DischargingService.class));
                        } else { // discharging service disabled -> use DischargingAlarmReceiver
                            context.sendBroadcast(new Intent(AppInfoHelper.BROADCAST_DISCHARGING_ALARM)); // start discharging alarm if enabled
                        }
                    }
                }
            }
        }
    }
}
