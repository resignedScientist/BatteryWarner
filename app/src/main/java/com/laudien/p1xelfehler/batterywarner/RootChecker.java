package com.laudien.p1xelfehler.batterywarner;

import android.content.Context;
import android.preference.PreferenceManager;

import eu.chainfire.libsuperuser.Shell;

public final class RootChecker {
    public static boolean isDeviceRooted() {
        return Shell.SU.available();
    }

    public static void enableCharging(Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_stop_charging), false)) {
            return;
        }
        if (!isDeviceRooted()) {
            return;
        }
        Shell.SU.run("echo 1 > /sys/class/power_supply/battery/charging_enabled");
    }

    public static void disableCharging(Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_stop_charging), false)) {
            return;
        }
        if (!isDeviceRooted()) {
            return;
        }
        Shell.SU.run("echo 0 > /sys/class/power_supply/battery/charging_enabled");
    }
}
