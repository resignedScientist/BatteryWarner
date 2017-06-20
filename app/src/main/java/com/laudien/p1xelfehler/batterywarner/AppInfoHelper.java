package com.laudien.p1xelfehler.batterywarner;

import android.os.Environment;

public final class AppInfoHelper {
    // Database
    public static final String DATABASE_HISTORY_PATH = Environment.getExternalStorageDirectory() + "/BatteryWarner";
    // package names
    public static final String PACKAGE_NAME_FREE = "com.laudien.p1xelfehler.batterywarner";
    public static final String PACKAGE_NAME_PRO = "com.laudien.p1xelfehler.batterywarner_pro";
    public static final boolean IS_PRO = BuildConfig.APPLICATION_ID.equals(PACKAGE_NAME_PRO);

    private AppInfoHelper() {
    }
}