package com.laudien.p1xelfehler.batterywarner.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Database.GraphChargeDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Receivers.BatteryAlarmManager;

import java.util.Locale;

public class GraphFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    //private static final String TAG = "GraphFragment";
    private SharedPreferences sharedPreferences;
    private GraphView graph_chargeCurve;
    private LineGraphSeries<DataPoint> series_chargeCurve, series_temp;
    private Viewport viewport_chargeCurve;
    private TextView textView_chargingTime;
    private int graphCounter;
    private CheckBox checkBox_percentage, checkBox_temp;
    private boolean graphEnabled;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        setHasOptionsMenu(true);
        sharedPreferences = getContext().getSharedPreferences(Contract.SHARED_PREFS, Context.MODE_PRIVATE);
        graph_chargeCurve = (GraphView) view.findViewById(R.id.graph_chargeCurve);
        viewport_chargeCurve = graph_chargeCurve.getViewport();
        textView_chargingTime = (TextView) view.findViewById(R.id.textView_chargingTime);
        graphEnabled = sharedPreferences.getBoolean(Contract.PREF_GRAPH_ENABLED, true);

        // line graphs (= series)
        series_chargeCurve = new LineGraphSeries<>();
        series_chargeCurve.setDrawBackground(true);
        series_temp = new LineGraphSeries<>();
        series_temp.setColor(Color.GREEN);

        // checkBoxes
        checkBox_percentage = (CheckBox) view.findViewById(R.id.checkbox_percentage);
        checkBox_temp = (CheckBox) view.findViewById(R.id.checkBox_temp);

        if (!graphEnabled) { // disable checkboxes if the graph is disabled
            checkBox_percentage.setEnabled(false);
            checkBox_temp.setEnabled(false);
        } else {
            checkBox_percentage.setChecked(sharedPreferences.getBoolean(Contract.PREF_CB_PERCENT, true));
            checkBox_temp.setChecked(sharedPreferences.getBoolean(Contract.PREF_CB_TEMP, false));
            checkBox_percentage.setOnCheckedChangeListener(this);
            checkBox_temp.setOnCheckedChangeListener(this);
        }

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
                if (isValueX) { // X-axis (time)
                    if (value == 0) {
                        graphCounter = 1;
                        return "0";
                    }
                    if (graphCounter++ % 2 != 0)
                        return super.formatLabel(value, true) + " min";
                    return "";
                } else if (checkBox_percentage.isChecked() ^ checkBox_temp.isChecked()) { // Y-axis (percent)
                    if (checkBox_percentage.isChecked())
                        return super.formatLabel(value, false) + "%";
                    if (checkBox_temp.isChecked())
                        return super.formatLabel(value, false) + "°C";
                }
                return super.formatLabel(value, false);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadChargeCurve();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Contract.BROADCAST_STATUS_CHANGED);
        getActivity().registerReceiver(dbChangedReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.edit()
                .putBoolean(Contract.PREF_CB_PERCENT, checkBox_percentage.isChecked())
                .putBoolean(Contract.PREF_CB_TEMP, checkBox_temp.isChecked())
                .apply();
        getActivity().unregisterReceiver(dbChangedReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.reload_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            Context applicationContext = getActivity().getApplicationContext();
            if (Contract.IS_PRO) {
                if (graphEnabled) {
                    reloadChargeCurve();
                    Toast.makeText(applicationContext, getString(R.string.graph_reloaded), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(applicationContext, getString(R.string.disabled_in_settings), Toast.LENGTH_SHORT).show();
                }
                return true;
            } else {
                Toast.makeText(applicationContext, "Sorry! :(", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void reloadChargeCurve() {
        // if not pro -> return
        if (!Contract.IS_PRO) {
            textView_chargingTime.setTextSize(20);
            textView_chargingTime.setText(getString(R.string.not_pro));
            checkBox_temp.setEnabled(false);
            checkBox_percentage.setEnabled(false);
            return;
        }
        // if disabled in settings -> return
        //Log.i(TAG, "graphEnabled = " + graphEnabled);
        if (!graphEnabled) {
            textView_chargingTime.setTextSize(18);
            textView_chargingTime.setText(getString(R.string.disabled_in_settings));
            return;
        }
        // remove the series from the graph view
        graph_chargeCurve.removeSeries(series_chargeCurve);
        graph_chargeCurve.removeSeries(series_temp);
        // load graph
        Intent batteryStatus = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;
        boolean isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
        boolean chargingModeEnabled = BatteryAlarmManager.isChargingModeEnabled(sharedPreferences, batteryStatus);
        boolean isFull = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE) == 100;
        int percentage;
        double temperature, time = 0;
        GraphChargeDbHelper dbHelper = new GraphChargeDbHelper(getContext());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] columns = {
                GraphChargeDbHelper.TABLE_COLUMN_TIME,
                GraphChargeDbHelper.TABLE_COLUMN_PERCENTAGE,
                GraphChargeDbHelper.TABLE_COLUMN_TEMP};
        Cursor cursor = database.query(GraphChargeDbHelper.TABLE_NAME, columns, null, null, null, null,
                "length(" + GraphChargeDbHelper.TABLE_COLUMN_TIME + "), " + GraphChargeDbHelper.TABLE_COLUMN_TIME);
        if (cursor.moveToFirst()) { // if the cursor has data
            do {
                time = getDoubleTime(cursor.getLong(0));
                percentage = cursor.getInt(1);
                temperature = (double) cursor.getInt(2) / 10;
                //Log.i(TAG, "Data read: time = " + time + "; percentage = " + percentage + "; temp = " + temperature);
                try {
                    series_chargeCurve.appendData(new DataPoint(time, percentage), false, 1000);
                    series_temp.appendData(new DataPoint(time, temperature), false, 1000);
                } catch (Exception e) { // if x has a lower value than the values on the graph -> reset graph
                    series_chargeCurve.resetData(new DataPoint[]{new DataPoint(time, percentage)});
                    series_temp.resetData(new DataPoint[]{new DataPoint(time, temperature)});
                    viewport_chargeCurve.setMaxX(1);
                }
            } while (cursor.moveToNext()); // while the cursor has data
        } else if (!isCharging) { // empty database and discharging -> no data yet + return
            textView_chargingTime.setText(getString(R.string.no_data));
            cursor.close();
            dbHelper.close();
            return;
        }
        cursor.close();
        dbHelper.close();
        // Is there enough data?
        boolean enoughData = time != 0;
        if (!enoughData) { // not enough data
            //time = 1;
            textView_chargingTime.setText(getString(R.string.not_enough_data));
        } else { // enough data
            viewport_chargeCurve.setMaxX(time); // set the viewport to the highest time
            // load the series into the graphView
            if (checkBox_percentage.isChecked())
                graph_chargeCurve.addSeries(series_chargeCurve);
            if (checkBox_temp.isChecked())
                graph_chargeCurve.addSeries(series_temp);
        }
        // Show user if charging and current charging type is disabled
        if (!chargingModeEnabled && isCharging) {
            textView_chargingTime.setText(getString(R.string.charging_type_disabled));
            return;
        }
        // Is the phone charging and is it NOT full charged?
        String timeString = getTimeString(time);
        if (isCharging && !isFull) { // charging and not fully charged -> "Charging... (time)"
            textView_chargingTime.setText(getString(R.string.charging) + " (" + timeString + ")");
        } else if (enoughData) { // discharging + ENOUGH data
            textView_chargingTime.setText(getString(R.string.charging_time) + ": " + timeString);
        }
    }

    private String getTimeString(double timeInMinutes) { // returns "hours h minutes min" or "minutes min"
        double minutes;
        if (timeInMinutes > 60) { // over an hour
            long hours = (long) timeInMinutes / 60;
            minutes = (timeInMinutes - hours * 60);
            if ((int) minutes == minutes) // if it is an .0 number
                return String.format(Locale.getDefault(), "%d h %d min", hours, (int) minutes);
            return String.format(Locale.getDefault(), "%d h %.1f min", hours, minutes);
        } else { // under an hour
            if ((int) timeInMinutes == timeInMinutes)
                return String.format(Locale.getDefault(), "%d min", (int) timeInMinutes);
            return String.format(Locale.getDefault(), "%.1f min", timeInMinutes);
        }
    }

    private double getDoubleTime(long timeInMillis) { // returns minutes as double
        return (double) Math.round(2 * (double) timeInMillis / 60000) / 2;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        LineGraphSeries series = null;
        switch (compoundButton.getId()) {
            case R.id.checkbox_percentage:
                series = series_chargeCurve;
                break;
            case R.id.checkBox_temp:
                series = series_temp;
                break;
        }
        if (series == null) return;
        if (b)
            graph_chargeCurve.addSeries(series);
        else
            graph_chargeCurve.removeSeries(series);
    }

    private BroadcastReceiver dbChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.i(TAG, "Status change received!");
            reloadChargeCurve();
        }
    };
}
