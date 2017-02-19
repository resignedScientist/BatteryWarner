package com.laudien.p1xelfehler.batterywarner;

import eu.chainfire.libsuperuser.Shell;

public final class RootChecker{
    public static boolean isDeviceRooted() {
       return Shell.SU.available();
    }

    public static void enableCharging() {
        if (!isDeviceRooted()){
            return;
        }
        Shell.SU.run("echo 1 > /sys/class/power_supply/battery/charging_enabled");
    }

    public static void disableCharging() {
        if (!isDeviceRooted()){
            return;
        }
        Shell.SU.run("echo 0 > /sys/class/power_supply/battery/charging_enabled");
    }
}
