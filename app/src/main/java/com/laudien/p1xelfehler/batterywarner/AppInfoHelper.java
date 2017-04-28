package com.laudien.p1xelfehler.batterywarner;

import android.os.Environment;

public final class AppInfoHelper {
    // Database
    public static final String DATABASE_HISTORY_PATH = Environment.getExternalStorageDirectory() + "/BatteryWarner";
    // custom broadcasts
    public static final String BROADCAST_BOTH_APPS_INSTALLED = "com.laudien.p1xelfehler.batterywarner.BOTH_APPS_INSTALLED";
    public static final String BROADCAST_DISCHARGING_ALARM = "com.laudien.p1xelfehler.batterywarner.DISCHARGING_ALARM";
    // package names
    public static final String PACKAGE_NAME_FREE = "com.laudien.p1xelfehler.batterywarner";
    public static final String PACKAGE_NAME_PRO = "com.laudien.p1xelfehler.batterywarner_pro";
    // activating pro-features
    public static final boolean IS_PRO = true;

    private AppInfoHelper() {
    }
}