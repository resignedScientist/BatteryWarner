package com.laudien.p1xelfehler.batterywarner;

import android.os.Environment;

public final class Contract {
    // Database
    public static final String DATABASE_HISTORY_PATH = Environment.getExternalStorageDirectory() + "/BatteryWarner";
    // custom broadcasts
    public static final String BROADCAST_ON_OFF_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_ON_OFF_CHANGED";
    public static final String BROADCAST_DB_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_DB_CHANGED";
    public static final String BROADCAST_BOTH_APPS_INSTALLED = "com.laudien.p1xelfehler.batterywarner.BOTH_APPS_INSTALLED";
    public static final String BROADCAST_DISCHARGING_ALARM = "com.laudien.p1xelfehler.batterywarner.DISCHARGING_ALARM";
    // package names
    public static final String PACKAGE_NAME_FREE = "com.laudien.p1xelfehler.batterywarner";
    public static final String PACKAGE_NAME_PRO = "com.laudien.p1xelfehler.batterywarner_pro";
    // activating pro-features
    public static final boolean IS_PRO = true;
    // value that means not set, unknown or error
    public static final int NO_STATE = -1;

    private Contract() {
    }
}
