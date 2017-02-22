package com.laudien.p1xelfehler.batterywarner;

import android.os.Environment;

public final class Contract {
    // Database
    public static final String DATABASE_HISTORY_PATH = Environment.getExternalStorageDirectory() + "/BatteryWarner";
    // custom broadcasts
    public static final String BROADCAST_ON_OFF_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_ON_OFF_CHANGED";
    public static final String BROADCAST_STATUS_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_STATUS_CHANGED";
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
