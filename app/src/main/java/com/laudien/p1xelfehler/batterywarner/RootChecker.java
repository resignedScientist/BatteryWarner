package com.laudien.p1xelfehler.batterywarner;

import android.content.Context;
import android.os.Looper;
import android.preference.PreferenceManager;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public final class RootChecker {
    public static boolean isDeviceRooted() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new InMainThreadException();
        }
        return Shell.SU.available();
    }

    public static void enableCharging(Context context) throws NotRootedException {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default))) {
            return;
        }
        if (!isDeviceRooted()) {
            throw new NotRootedException();
        }
        Shell.SU.run("echo 1 > /sys/class/power_supply/battery/charging_enabled");
    }

    static void disableCharging(Context context) throws NotRootedException {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_stop_charging), context.getResources().getBoolean(R.bool.pref_stop_charging_default))) {
            return;
        }
        if (!isDeviceRooted()) {
            throw new NotRootedException();
        }
        Shell.SU.run("echo 0 > /sys/class/power_supply/battery/charging_enabled");
    }

    public static boolean isChargingEnabled() throws NotRootedException, BatteryFileNotFoundException {
        if (!isDeviceRooted()) {
            throw new NotRootedException();
        }
        List output = Shell.SU.run("cat /sys/class/power_supply/battery/charging_enabled");
        if (output != null && !output.isEmpty()) {
            return output.get(0).equals("1");
        } else {
            throw new BatteryFileNotFoundException();
        }
    }

    public static class NotRootedException extends Exception {
        private NotRootedException() {
            super("The device is not rooted!");
        }
    }

    private static class InMainThreadException extends RuntimeException {
        private InMainThreadException() {
            super("Root calls must be done outside of the main thread!");
        }
    }

    public static class BatteryFileNotFoundException extends Exception {
        private BatteryFileNotFoundException() {
            super("The battery file was not found. Stop charging does not work with this device!");
        }
    }
}
