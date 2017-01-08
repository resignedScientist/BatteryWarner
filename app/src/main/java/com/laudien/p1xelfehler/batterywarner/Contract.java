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
    public static final String PREF_ALREADY_NOTIFIED = "notificationShown";
    public static final String PREF_FASTER_INTERVAL = "fasterInterval";
    public static final String PREF_CB_PERCENT = "checkBoxPercent";
    public static final String PREF_CB_TEMP = "checkBoxDegrees";
    public static final String PREF_DARK_THEME = "darkThemeEnabled";
    // min and max values
    public static final int WARNING_HIGH_MIN = 60;
    public static final int WARNING_LOW_MAX = 40;
    public static final int WARNING_LOW_MIN = 5;
    // charge checking intervals
    public static final int INTERVAL_DISCHARGING_VERY_LONG = 3600000; // 1 hour
    public static final int INTERVAL_DISCHARGING_LONG = 1800000; // 30 minutes
    public static final int INTERVAL_DISCHARGING_SHORT = 900000; // 15 minutes
    public static final int INTERVAL_DISCHARGING_VERY_SHORT = 60000; // 1 minute
    // default values
    public static final int DEF_WARNING_LOW = 20;
    public static final int DEF_WARNING_HIGH = 80;
    // intent request codes
    public static final int PICK_SOUND_REQUEST = 1;
    // custom broadcasts
    public static final String BROADCAST_ON_OFF_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_ON_OFF_CHANGED";
    public static final String BROADCAST_STATUS_CHANGED = "com.laudien.p1xelfehler.batterywarner.BROADCAST_STATUS_CHANGED";
    // notification ids
    public static final int NOTIFICATION_ID_SILENT_MODE = 1337;
    public static final int NOTIFICATION_ID_BATTERY_WARNING = 1338;
    // package names
    public static final String PACKAGE_NAME_FREE = "com.laudien.p1xelfehler.batterywarner";
    public static final String PACKAGE_NAME_PRO = "com.laudien.p1xelfehler.batterywarner_pro";
    // activating pro-features
    public static final boolean IS_PRO = true;
    // the rest
    public static final int NO_STATE = -1;

    private Contract() {
    }
}
