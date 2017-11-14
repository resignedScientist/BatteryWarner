package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

public class TemperatureConverter {
    private static double convertCelsiusToFahrenheit(double celsius) {
        return ((celsius * 9) / 5) + 32;
    }

    public static String getCorrectTemperatureString(Context context, double tempInCelsius) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String tempUnitChosen = sharedPreferences.getString(context.getString(R.string.pref_temp_unit), context.getString(R.string.pref_temp_unit_default));
        switch (tempUnitChosen) {
            case "0":
                return String.format(
                        Locale.getDefault(),
                        "%.1f %s",
                        tempInCelsius,
                        "°C"
                );
            case "1":
                return String.format(
                        Locale.getDefault(),
                        "%.1f %s",
                        convertCelsiusToFahrenheit(tempInCelsius),
                        "°F"
                );
            default:
                return null;
        }
    }
}
