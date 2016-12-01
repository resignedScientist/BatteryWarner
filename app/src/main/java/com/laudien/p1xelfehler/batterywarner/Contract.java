package com.laudien.p1xelfehler.batterywarner;

public final class Contract {
    // shared preferences
    public static final String SHARED_PREFS = "BatteryWarner";
    public static final String PREF_FIRST_START = "FirstStart";
    public static final String PREF_INTENT_TIME = "PendingIntentTime";
    public static final String PREF_IS_ENABLED = "IsEnabled";
    public static final String PREF_USB_ENABLED = "usbEnabled";
    public static final String PREF_AC_ENABLED = "acEnabled";
    public static final String PREF_WIRELESS_ENABLED = "wirelessEnabled";
    public static final String PREF_WARNING_LOW_ENABLED = "warningLowEnabled";
    public static final String PREF_WARNING_HIGH_ENABLED = "warningHighEnabled";
    public static final String PREF_WARNING_LOW = "warningLow";
    public static final String PREF_WARNING_HIGH = "warningHigh";
    public static final String PREF_SOUND_URI = "savedSoundUri";
    public static final String PREF_GRAPH_ENABLED = "graphEnabled";
    public static final String PREF_LAST_PERCENTAGE = "lastPercentage";
    public static final String PREF_GRAPH_TIME = "graphTime";

    // min and max values
    public static final int WARNING_HIGH_MIN = 60;
    public static final int WARNING_LOW_MAX = 40;

    // charge checking intervals
    public static final int INTERVAL_CHARGING = 60000; // 1 minute
    public static final int INTERVAL_DISCHARGING_VERY_LONG = 3600000; // 1 hour
    public static final int INTERVAL_DISCHARGING_LONG = 1800000; // 30 minutes
    public static final int INTERVAL_DISCHARGING_SHORT = 900000; // 15 minutes
    public static final int INTERVAL_DISCHARGING_VERY_SHORT = 180000; // 3 minutes

    // default values
    public static final int DEF_WARNING_LOW = 20;
    public static final int DEF_WARNING_HIGH = 80;

    // intent request codes
    public static final int PICK_SOUND_REQUEST = 1;
}
