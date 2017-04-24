package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.BatteryInfoNotificationService;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;
import com.laudien.p1xelfehler.batterywarner.Services.DischargingService;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.BROADCAST_DISCHARGING_ALARM;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.NotificationHelper.ID_GRANT_ROOT;
import static java.text.DateFormat.SHORT;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

/**
 * A BroadcastReceiver that is called by the system if the app has been updated.
 * It starts some services if necessary and asks for root permission if some root settings are used.
 * Does only work after the intro was finished.
 */
public class AppUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.getBoolean(context.getString(R.string.pref_first_start), context.getResources().getBoolean(R.bool.pref_first_start_default)))
                return; // return if intro was not finished

            // start info notification service
            context.startService(new Intent(context, BatteryInfoNotificationService.class));

            Intent batteryStatus = context.registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
            if (batteryStatus == null) {
                return;
            }
            boolean isCharging = batteryStatus.getIntExtra(EXTRA_PLUGGED, -1) != 0;
            if (isCharging) { // charging
                context.startService(new Intent(context, ChargingService.class));
            } else { // discharging
                DischargingAlarmReceiver.cancelDischargingAlarm(context);
                context.sendBroadcast(new Intent(BROADCAST_DISCHARGING_ALARM));
                boolean serviceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
                if (serviceEnabled) {
                    context.startService(new Intent(context, DischargingService.class));
                }
            }
            // patch old time strings
            try {
                String timeString = sharedPreferences.getString(context.getString(R.string.pref_smart_charging_time), null);
                if (timeString != null) {
                    DateFormat dateFormat = java.text.DateFormat.getTimeInstance(SHORT, Locale.getDefault());
                    Date date = dateFormat.parse(timeString);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    int hours = calendar.get(HOUR_OF_DAY);
                    int minutes = calendar.get(MINUTE);
                    calendar = Calendar.getInstance();
                    calendar.set(HOUR_OF_DAY, hours);
                    calendar.set(MINUTE, minutes);
                    Log.d(getClass().getSimpleName(), date.toString());
                    sharedPreferences.edit().remove(context.getString(R.string.pref_smart_charging_time))
                            .putLong(context.getString(R.string.pref_smart_charging_time), calendar.getTimeInMillis())
                            .apply();
                }
            } catch (Exception ignored) {
            }
            // show notification if not rooted anymore
            NotificationHelper.showNotification(context, ID_GRANT_ROOT);
        }
    }
}
