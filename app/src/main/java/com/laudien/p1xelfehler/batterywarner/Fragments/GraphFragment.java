package com.laudien.p1xelfehler.batterywarner.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receiver.BatteryAlarmReceiver;

public class GraphFragment extends Fragment {
    private static final String TAG = "GraphFragment";
    private SharedPreferences sharedPreferences;
    private GraphView graph_chargeCurve;
    private LineGraphSeries<DataPoint> series_chargeCurve;
    private Viewport viewport_chargeCurve;
    private TextView textView_chargingTime;
    private long lastTime;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        sharedPreferences = getContext().getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        graph_chargeCurve = (GraphView) view.findViewById(R.id.graph_chargeCurve);
        viewport_chargeCurve = graph_chargeCurve.getViewport();
        textView_chargingTime = (TextView) view.findViewById(R.id.textView_chargingTime);
        lastTime = 1;

        // y bounds
        viewport_chargeCurve.setYAxisBoundsManual(true);
        viewport_chargeCurve.setMinY(0);
        viewport_chargeCurve.setMaxY(100);

        // x bounds
        viewport_chargeCurve.setXAxisBoundsManual(true);
        viewport_chargeCurve.setMinX(0);
        viewport_chargeCurve.setMaxX(1);

        graph_chargeCurve.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) // X-axis (time)
                    if (value == lastTime || value == 1 || value == 0)
                        return super.formatLabel(value, true) + " min";
                    else
                        return "";
                else // Y-axis (percent)
                    return super.formatLabel(value, false) + "%";
            }
        });

        series_chargeCurve = new LineGraphSeries<>();
        series_chargeCurve.setDrawBackground(true);
        graph_chargeCurve.addSeries(series_chargeCurve);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadChargeCurve();
    }

    public void reloadChargeCurve() {
        // 1. if not pro -> return
        if (!Contract.IS_PRO) {
            graph_chargeCurve.setVisibility(View.INVISIBLE);
            textView_chargingTime.setTextSize(18);
            textView_chargingTime.setText(getString(R.string.disabled_in_settings));
            return;
        }
        // 2. if disabled in settings -> return
        if (!sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true)) {
            textView_chargingTime.setTextSize(20);
            textView_chargingTime.setText(getString(R.string.not_pro));
        }
        boolean charging = BatteryAlarmReceiver.isCharging(getContext()); // get the charging state
        // 3. load graph
        long time;
        int percentage;
        GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] columns = {GraphChargeDbHelper.TABLE_COLUMN_TIME, GraphChargeDbHelper.TABLE_COLUMN_PERCENTAGE};
        Cursor cursor = database.query(GraphChargeDbHelper.TABLE_NAME, columns, null, null, null, null,
                "length(" + GraphChargeDbHelper.TABLE_COLUMN_TIME + "), " + GraphChargeDbHelper.TABLE_COLUMN_TIME);
        if (cursor.moveToFirst()) { // if the cursor has data
            do {
                time = cursor.getLong(0);
                percentage = cursor.getInt(1);
                Log.i(TAG, "Data read: time = " + time + "; percentage = " + percentage);
                try {
                    series_chargeCurve.appendData(new DataPoint(time / 60000, percentage), false, 1000);
                    lastTime = time / 60000;
                } catch (Exception e) { // if x has a lower value than the values on the graph -> reset graph
                    series_chargeCurve.resetData(new DataPoint[]{new DataPoint(time / 60000, percentage)});
                    viewport_chargeCurve.setMaxX(1);
                    lastTime = 1;
                }
            } while (cursor.moveToNext()); // while the cursor has data
        } else { // empty database -> return
            if (charging) // "Charging..."
                textView_chargingTime.setText(getString(R.string.charging));
            else // "Not data"
                textView_chargingTime.setText(getString(R.string.no_data));
            return;
        }
        // 4. Is the phone charging and is it NOT full charged?
        if (charging && percentage != 100) { // charging and not fully charged -> "Charging..."
            textView_chargingTime.setText(getString(R.string.charging));
        } else if (time != 0) { // discharging or full charged + there IS enough data:
            // Calculate the charging time string
            String timeString = getString(R.string.charging_time) + ": ";
            long minutes;
            if (time > 3600000) { // over an hour
                long hours = time / 3600000;
                minutes = (time - hours * 3600000) / 60000;
                timeString += hours + " h, ";
            } else // under an hour
                minutes = time / 60000;
            timeString += minutes + " min";
            // Show the time in the textView
            textView_chargingTime.setText(timeString);
        } else { // not enough data -> "not enough data"
            textView_chargingTime.setText(getString(R.string.not_enough_data));
        }
        // 5. Are there 3 or more values in the database?
        if (time > 61000) {
            viewport_chargeCurve.setMaxX(time / 60000); // set the viewport to the highest time
        }
    }
}
