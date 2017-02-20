package com.laudien.p1xelfehler.batterywarner;

import android.content.Context;
import android.preference.PreferenceManager;

import eu.chainfire.libsuperuser.Shell;

public final class RootChecker {
    public static boolean isDeviceRooted() {
        return Shell.SU.available();
    }

    public static boolean enableCharging(Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_stop_charging), false)) {
            return false;
        }
        if (!isDeviceRooted()) {
            return false;
        }
        Shell.SU.run("echo 1 > /sys/class/power_supply/battery/charging_enabled");
        return true;
    }

    public static boolean disableCharging(Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_stop_charging), false)) {
            return false;
        }
        if (!isDeviceRooted()) {
            return false;
        }
        Shell.SU.run("echo 0 > /sys/class/power_supply/battery/charging_enabled");
        return true;
    }

    public static boolean isChargingEnabled() throws NotRootedException {
        if (!isDeviceRooted()) {
            throw new NotRootedException();
        }
        return Shell.SU.run("cat /sys/class/power_supply/battery/charging_enabled").get(0).equals("1");
    }
}
