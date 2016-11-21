package com.example.laudien.batterywarner;

import static android.app.AlarmManager.INTERVAL_FIFTEEN_MINUTES;
import static android.app.AlarmManager.INTERVAL_HALF_HOUR;

public class Contract {
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

    // values
    public static final int WARNING_HIGH_MIN = 60;
    public static final int WARNING_LOW_MAX = 40;
    public static final long INTERVAL_CHARGING = 1000 * 60;
    public static final long INTERVAL_DISCHARGING_LONG = INTERVAL_HALF_HOUR;
    public static final long INTERVAL_DISCHARGING_SHORT = INTERVAL_FIFTEEN_MINUTES;
    public static final long INTERVAL_DISCHARGING_VERY_SHORT = INTERVAL_CHARGING;

    // default values
    public static final int DEF_WARNING_LOW = 20;
    public static final int DEF_WARNING_HIGH = 80;
}
