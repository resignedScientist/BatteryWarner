package com.laudien.p1xelfehler.batterywarner.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import java.text.DateFormat;
import java.util.Locale;

/**
 * This Class saves information about the charging curve. It can show an info dialog to the user.
 */
class InfoObject {
    private double timeInMinutes, maxTemp, minTemp, percentCharged;
    private long startTime, endTime;

    /**
     * Constructor of the InfoObject. All values must be provided.
     *
     * @param endTime        The time the graph was created.
     * @param timeInMinutes  The time of charging in minutes.
     * @param maxTemp        The maximal battery temperature while charging.
     * @param minTemp        The minimal battery temperature while charging.
     * @param percentCharged The battery level difference from the beginning to the end of charging in percent.
     */
    InfoObject(long startTime, long endTime, double timeInMinutes, double maxTemp, double minTemp, double percentCharged) {
        updateValues(startTime, endTime, timeInMinutes, maxTemp, minTemp, percentCharged);
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
     * @param startTime      The time of the first point in the graph.
     * @param endTime        The time of the last point in the graph.
     * @param timeInMinutes  The time of charging in minutes.
     * @param maxTemp        The maximal battery temperature while charging.
     * @param minTemp        The minimal battery temperature while charging.
     * @param percentCharged The battery level difference from the beginning to the end of charging in percent.
     */
    void updateValues(long startTime, long endTime, double timeInMinutes, double maxTemp, double minTemp, double percentCharged) {
        this.startTime = startTime;
        updateValues(endTime, timeInMinutes, maxTemp, minTemp, percentCharged);
    }

    void updateValues(long endTime, double timeInMinutes, double maxTemp, double minTemp, double percentCharged) {
        this.endTime = endTime;
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
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_graph_info);
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        // close button
        Button btn_close = (Button) dialog.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        // charging time
        TextView textView_totalTime = (TextView) dialog.findViewById(R.id.textView_totalTime);
        textView_totalTime.setText(String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_charging_time),
                getTimeString(context))
        );
        // start time
        TextView textView_startTime = (TextView) dialog.findViewById(R.id.textView_startTime);
        textView_startTime.setText(String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_startTime),
                dateFormat.format(startTime)
        ));
        // end time
        TextView textView_endTime = (TextView) dialog.findViewById(R.id.textView_endTime);
        textView_endTime.setText(String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_endTime),
                dateFormat.format(endTime)
        ));
        // charging speed
        TextView textView_speed = (TextView) dialog.findViewById(R.id.textView_speed);
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
        // max temperature
        TextView textView_maxTemp = (TextView) dialog.findViewById(R.id.textView_maxTemp);
        textView_maxTemp.setText(String.format(
                Locale.getDefault(),
                "%s: %.1f°C",
                context.getString(R.string.info_max_temp),
                maxTemp)
        );
        // min temperature
        TextView textView_minTemp = (TextView) dialog.findViewById(R.id.textView_minTemp);
        textView_minTemp.setText(String.format(
                Locale.getDefault(),
                "%s: %.1f°C",
                context.getString(R.string.info_min_temp),
                minTemp)
        );
        // show dialog
        dialog.show();
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
