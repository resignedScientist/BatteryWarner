package com.laudien.p1xelfehler.batterywarner.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

public class InfoObject {
    private double timeInMinutes, maxTemp, minTemp, percentCharged;

    public InfoObject(double timeInMinutes, double maxTemp, double minTemp, double percentCharged) {
        updateValues(timeInMinutes, maxTemp, minTemp, percentCharged);
    }

    private static String[] getTimeFormats(Context context) {
        String[] formats = new String[4];
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String timeFormat = sharedPreferences.getString(context.getString(R.string.pref_time_format), "0");
        switch (timeFormat) {
            case "0":
                formats[0] = "%d h %.0f min";
                formats[1] = "%.0f min";
                formats[2] = "%.0f min";
                formats[3] = "false";
                break;
            case "1":
                formats[0] = "%d h %.1f min";
                formats[1] = "%.1f min";
                formats[2] = "%.1f min";
                formats[3] = "false";
                break;
            case "2":
                formats[0] = "%d h %.0f min %.0f s";
                formats[1] = "%.0f min %.0f s";
                formats[2] = "%.0f s";
                formats[3] = "true";
                break;
        }
        return formats;
    }

    public static String getZeroTimeString(Context context) {
        String[] formats = getTimeFormats(context);
        return String.format(Locale.getDefault(), formats[2], 0f);
    }

    public void updateValues(double timeInMinutes, double maxTemp, double minTemp, double percentCharged) {
        this.timeInMinutes = timeInMinutes;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.percentCharged = percentCharged;
    }

    public String getTimeString(Context context) {
        String[] formats = getTimeFormats(context);
        boolean useSeconds = Boolean.valueOf(formats[3]);
        if (timeInMinutes > 60) { // over an hour
            long hours = (long) timeInMinutes / 60;
            double minutes = (timeInMinutes - hours * 60);
            if (useSeconds) {
                double minutes_floor = Math.floor(minutes);
                double seconds = (minutes - minutes_floor) * 60;
                return String.format(Locale.getDefault(), formats[0], hours, minutes_floor, seconds);
            }
            return String.format(Locale.getDefault(), formats[0], hours, minutes);
        } else if (timeInMinutes > 1) { // under an hour, over a minute
            if (useSeconds) {
                double minutes = Math.floor(timeInMinutes);
                double seconds = (timeInMinutes - minutes) * 60;
                return String.format(Locale.getDefault(), formats[1], minutes, seconds);
            }
            return String.format(Locale.getDefault(), formats[1], timeInMinutes);
        } else { // under a minute
            if (useSeconds) {
                return String.format(Locale.getDefault(), formats[2], timeInMinutes * 60);
            }
            return String.format(Locale.getDefault(), formats[2], timeInMinutes);
        }
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public double getMinTemp() {
        return minTemp;
    }

    public double getTimeInMinutes() {
        return timeInMinutes;
    }

    public double getPercentCharged() {
        return percentCharged;
    }
}