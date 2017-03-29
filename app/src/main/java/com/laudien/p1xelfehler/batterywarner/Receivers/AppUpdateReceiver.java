package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;
import com.laudien.p1xelfehler.batterywarner.Services.DischargingService;

/**
 * A BroadcastReceiver that is called by the system if the app has been updated.
 * It starts some services if necessary and asks for root permission if some root settings are used.
 * Does only work after the intro was finished.
 */
public class AppUpdateReceiver extends BroadcastReceiver {

    //private final String TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) return;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default)))
            return; // return if intro was not finished

        Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
        if (isCharging) { // charging
            context.startService(new Intent(context, ChargingService.class));
        } else { // discharging
            DischargingAlarmReceiver.cancelDischargingAlarm(context);
            context.sendBroadcast(new Intent(Contract.BROADCAST_DISCHARGING_ALARM));
            boolean serviceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
            if (serviceEnabled) {
                context.startService(new Intent(context, DischargingService.class));
            }
        }

        // patch old time strings
        try {
            String timeString = sharedPreferences.getString(context.getString(R.string.pref_smart_charging_time), null);
            if (timeString != null){
                sharedPreferences.edit().remove(context.getString(R.string.smart_charging_time))
                        .putLong(context.getString(R.string.smart_charging_time), Long.parseLong(timeString))
                        .apply();
            }
        } catch (Exception ignored){}

        // show notification if not rooted anymore
        NotificationBuilder.showNotification(context, NotificationBuilder.ID_GRANT_ROOT);
    }
}
