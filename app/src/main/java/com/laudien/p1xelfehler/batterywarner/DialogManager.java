package com.laudien.p1xelfehler.batterywarner;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.Activities.InfoObject;

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
                "Maximale Temperatur", infoObject.getMaxTemp()));
        TextView textView_minTemp = (TextView) view.findViewById(R.id.textView_minTemp);
        textView_minTemp.setText(String.format(Locale.getDefault(), "%s: %.1f°C",
                "Minimale Temperatur", infoObject.getMinTemp()));
        new AlertDialog.Builder(activity)
                .setTitle("Graph info")
                .setView(view)
                .setCancelable(true)
                .setPositiveButton("Close", null)
                .create()
                .show();
    }
}
