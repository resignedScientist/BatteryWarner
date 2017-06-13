package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.receivers.DischargingAlarmReceiver;
import com.laudien.p1xelfehler.batterywarner.services.ChargingService;
import com.laudien.p1xelfehler.batterywarner.services.DischargingService;

import static android.os.BatteryManager.EXTRA_PLUGGED;

public class ServiceHelper {
    public static final byte ID_CHARGING = 0;
    public static final byte ID_DISCHARGING = 1;

    public static void startService(Context context, @Nullable SharedPreferences sharedPreferences, byte serviceID) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        boolean isEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_is_enabled), context.getResources().getBoolean(R.bool.pref_is_enabled_default));
        if (isEnabled) {
            DischargingAlarmReceiver.cancelDischargingAlarm(context);
            if (serviceID == ID_CHARGING) {
                boolean warningHighEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_high_enabled), context.getResources().getBoolean(R.bool.pref_warning_high_enabled_default));
                if (warningHighEnabled) {
                    Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    boolean isCharging = batteryStatus == null || batteryStatus.getIntExtra(EXTRA_PLUGGED, -1) != 0;
                    if (isCharging) {
                        startService(context, new Intent(context, ChargingService.class));
                    }
                }
            } else if (serviceID == ID_DISCHARGING) {
                boolean infoNotificationEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_info_notification_enabled), context.getResources().getBoolean(R.bool.pref_info_notification_enabled_default));
                boolean dischargingServiceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_discharging_service_enabled), context.getResources().getBoolean(R.bool.pref_discharging_service_enabled_default));
                boolean warningLowEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_warning_low_enabled), context.getResources().getBoolean(R.bool.pref_warning_low_enabled_default));
                if (infoNotificationEnabled || dischargingServiceEnabled) {
                    startService(context, new Intent(context, DischargingService.class));
                } else if (warningLowEnabled) {
                    context.sendBroadcast(new Intent(context, DischargingAlarmReceiver.class));
                }
            } else {
                throw new RuntimeException("Unknown service id!");
            }
        }
    }

    private static void startService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void restartService(Context context, @Nullable SharedPreferences sharedPreferences, byte serviceID) {
        Intent intent;
        if (serviceID == ID_CHARGING) {
            intent = new Intent(context, ChargingService.class);
        } else if (serviceID == ID_DISCHARGING) {
            intent = new Intent(context, DischargingService.class);
        } else {
            throw new RuntimeException("Unknown service id!");
        }
        context.stopService(intent);
        startService(context, sharedPreferences, serviceID);
    }
}
