package com.laudien.p1xelfehler.batterywarner;

public final class Contract {
    // min and max values
    public static final int WARNING_HIGH_MIN = 60;
    public static final int WARNING_LOW_MAX = 40;
    public static final int WARNING_LOW_MIN = 5;
    // default values
    public static final int DEF_WARNING_LOW = 20;
    public static final int DEF_WARNING_HIGH = 80;
    // intent request codes
    public static final int PICK_SOUND_REQUEST = 1;
    // custom broadcasts
    public static final String BROADCAST_ON_OFF_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_ON_OFF_CHANGED";
    public static final String BROADCAST_STATUS_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_STATUS_CHANGED";
    // package names
    public static final String PACKAGE_NAME_FREE = "com.laudien.p1xelfehler.batterywarner";
    public static final String PACKAGE_NAME_PRO = "com.laudien.p1xelfehler.batterywarner_pro";
    // activating pro-features
    public static final boolean IS_PRO = false;
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
