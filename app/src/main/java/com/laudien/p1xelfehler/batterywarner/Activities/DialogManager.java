package com.laudien.p1xelfehler.batterywarner.Activities;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

public class DialogManager {
    private static DialogManager instance;

    private DialogManager() {

    }

    public static DialogManager getInstance() {
        if (instance == null) {
            instance = new DialogManager();
        }
        return instance;
    }

    public void showInfoDialog(Activity activity, InfoObject infoObject) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_graph_info, null);
        TextView textView_totalTime = (TextView) view.findViewById(R.id.textView_totalTime);
        textView_totalTime.setText(String.format(Locale.getDefault(), "%s: %s",
                activity.getString(R.string.charging_time), infoObject.getTimeString(activity)));
        TextView textView_maxTemp = (TextView) view.findViewById(R.id.textView_maxTemp);
        textView_maxTemp.setText(String.format(Locale.getDefault(), "%s: %.1f°C",
                activity.getString(R.string.max_temp), infoObject.getMaxTemp()));
        TextView textView_minTemp = (TextView) view.findViewById(R.id.textView_minTemp);
        textView_minTemp.setText(String.format(Locale.getDefault(), "%s: %.1f°C",
                activity.getString(R.string.min_temp), infoObject.getMinTemp()));
        TextView textView_speed = (TextView) view.findViewById(R.id.textView_speed);
        textView_speed.setText(String.format(Locale.getDefault(), "%s: %.2f%%/h",
                activity.getString(R.string.charging_speed),
                infoObject.getPercentCharged() * 60 / infoObject.getTimeInMinutes()));
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.graph_info))
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(activity.getString(R.string.close), null)
                .setIcon(R.mipmap.ic_launcher)
                .create()
                .show();
    }
}
