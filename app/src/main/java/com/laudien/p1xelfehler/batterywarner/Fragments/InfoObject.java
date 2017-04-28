package com.laudien.p1xelfehler.batterywarner.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import java.text.DateFormat;
import java.util.Locale;

/**
 * This Class saves information about the charging curve. It can show an info dialog to the user.
 */
class InfoObject {
    private double timeInMinutes, maxTemp, minTemp, percentCharged;
    private long creationTime;

    /**
     * Constructor of the InfoObject. All values must be provided.
     *
     * @param creationTime   The time the graph was created.
     * @param timeInMinutes  The time of charging in minutes.
     * @param maxTemp        The maximal battery temperature while charging.
     * @param minTemp        The minimal battery temperature while charging.
     * @param percentCharged The battery level difference from the beginning to the end of charging in percent.
     */
    InfoObject(long creationTime, double timeInMinutes, double maxTemp, double minTemp, double percentCharged) {
        updateValues(creationTime, timeInMinutes, maxTemp, minTemp, percentCharged);
    }

    private static String[] getTimeFormats(Context context) {
        String[] formats = new String[4];
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String timeFormat = sharedPreferences.getString(context.getString(R.string.pref_time_format), context.getString(R.string.pref_time_format_default));
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

    /**
     * Returns the time string for zero seconds. Uses the time format set by the user in the settings.
     *
     * @param context An instance of the Context class.
     * @return Returns the time string for zero seconds.
     */
    static String getZeroTimeString(Context context) {
        String[] formats = getTimeFormats(context);
        return String.format(Locale.getDefault(), formats[2], 0f);
    }

    /**
     * With that method you can update this instance of the InfoObject without creating a new one.
     *
     * @param creationTime   The time the graph was created.
     * @param timeInMinutes  The time of charging in minutes.
     * @param maxTemp        The maximal battery temperature while charging.
     * @param minTemp        The minimal battery temperature while charging.
     * @param percentCharged The battery level difference from the beginning to the end of charging in percent.
     */
    void updateValues(long creationTime, double timeInMinutes, double maxTemp, double minTemp, double percentCharged) {
        this.creationTime = creationTime;
        this.timeInMinutes = timeInMinutes;
        this.maxTemp = maxTemp;
        this.minTemp = minTemp;
        this.percentCharged = percentCharged;
    }

    /**
     * Shows the info dialog with all the information of the charging curve to the user.
     *
     * @param context An instance of the Context class.
     */
    void showDialog(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_graph_info, null);
        TextView textView_totalTime = (TextView) view.findViewById(R.id.textView_totalTime);
        textView_totalTime.setText(String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_charging_time),
                getTimeString(context))
        );
        String date = context.getString(R.string.health_unknown);
        if (creationTime > 1000000000) {
            date = DateFormat.getDateInstance(DateFormat.SHORT).format(creationTime);
        }
        TextView textView_date = (TextView) view.findViewById(R.id.textView_date);
        textView_date.setText(String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_date),
                date
        ));
        TextView textView_speed = (TextView) view.findViewById(R.id.textView_speed);
        double speed = percentCharged * 60 / timeInMinutes;
        if (Double.isNaN(speed)) {
            textView_speed.setText(String.format(
                    Locale.getDefault(),
                    "%s: %s %%/h",
                    context.getString(R.string.info_charging_speed),
                    "N/A")
            );
        } else {
            textView_speed.setText(String.format(
                    Locale.getDefault(),
                    "%s: %.2f %%/h",
                    context.getString(R.string.info_charging_speed),
                    speed)
            );
        }
        TextView textView_maxTemp = (TextView) view.findViewById(R.id.textView_maxTemp);
        textView_maxTemp.setText(String.format(
                Locale.getDefault(),
                "%s: %.1f°C",
                context.getString(R.string.info_max_temp),
                maxTemp)
        );
        TextView textView_minTemp = (TextView) view.findViewById(R.id.textView_minTemp);
        textView_minTemp.setText(String.format(
                Locale.getDefault(),
                "%s: %.1f°C",
                context.getString(R.string.info_min_temp),
                minTemp)
        );
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.dialog_title_graph_info))
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(context.getString(R.string.dialog_button_close), null)
                .setIcon(R.mipmap.ic_launcher)
                .create()
                .show();
    }

    /**
     * Returns the time string with the charging time. The format is defined by the user in the settings.
     *
     * @param context An instance of the Context class.
     * @return Returns the time string with the charging time.
     */
    String getTimeString(Context context) {
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

    /**
     * Returns the time of charging in minutes.
     *
     * @return Returns the time of charging in minutes.
     */
    double getTimeInMinutes() {
        return timeInMinutes;
    }
}
