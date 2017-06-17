package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
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
            if (serviceID == ID_CHARGING) {
                boolean chargingServiceEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_charging_service_enabled), context.getResources().getBoolean(R.bool.pref_charging_service_enabled_default));
                if (chargingServiceEnabled) {
                    Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    boolean isCharging = batteryStatus == null || batteryStatus.getIntExtra(EXTRA_PLUGGED, -1) != 0;
                    int batteryLevel = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
                    if (isCharging && batteryLevel < 100) {
                        startService(context, new Intent(context, ChargingService.class));
                    }
                }
            } else if (serviceID == ID_DISCHARGING) {
                startService(context, new Intent(context, DischargingService.class));
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

    private static void stopDischargingService(Context context) {
        context.stopService(new Intent(context, DischargingService.class));
    }

    public static void restartDischargingService(Context context, @Nullable SharedPreferences sharedPreferences) {
        stopDischargingService(context);
        startService(context, sharedPreferences, ID_DISCHARGING);
    }
}
