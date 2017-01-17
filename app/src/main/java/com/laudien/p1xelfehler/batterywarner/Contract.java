package com.laudien.p1xelfehler.batterywarner;

import android.os.Environment;

public final class Contract {
    // Database
    public static final String DATABASE_HISTORY_PATH = Environment.getExternalStorageDirectory() + "/BatteryWarner";
    // permission request codes
    public static final int REQUEST_SAVE_GRAPH = 10;
    public static final int REQUEST_LOAD_GRAPH = 20;
    // default values
    public static final int DEF_WARNING_LOW = 20;
    public static final int DEF_WARNING_HIGH = 80;
    // custom broadcasts
    public static final String BROADCAST_ON_OFF_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_ON_OFF_CHANGED";
    public static final String BROADCAST_STATUS_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_STATUS_CHANGED";
    // package names
    public static final String PACKAGE_NAME_FREE = "com.laudien.p1xelfehler.batterywarner";
    public static final String PACKAGE_NAME_PRO = "com.laudien.p1xelfehler.batterywarner_pro";
    // activating pro-features
    public static final boolean IS_PRO = true;
    // the rest
    public static final int NO_STATE = -1;
    // Shared Preferences that are also needed here
    static final String PREF_WARNING_HIGH = "warningHigh";
    static final String PREF_WARNING_LOW = "warningLow";
    // charge checking intervals
    static final int INTERVAL_DISCHARGING_VERY_LONG = 3600000; // 1 hour
    static final int INTERVAL_DISCHARGING_LONG = 1800000; // 30 minutes
    static final int INTERVAL_DISCHARGING_SHORT = 900000; // 15 minutes
    static final int INTERVAL_DISCHARGING_VERY_SHORT = 60000; // 1 minute

    private Contract() {
    }
}
