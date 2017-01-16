package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.ColorUtils;
import android.util.TypedValue;
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
import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

public class GraphFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    //private static final String TAG = "GraphFragment";
    private SharedPreferences sharedPreferences;
    private String timeFormat, timeFormat_underHour, timeFormat_underMinute;
    private GraphView graph_chargeCurve;
    private LineGraphSeries<DataPoint> series_chargeCurve, series_temp;
    private Viewport viewport_chargeCurve;
    private TextView textView_chargingTime;
    private int graphCounter, color_percentage, color_percentageBackground, color_temperature;
    private CheckBox checkBox_percentage, checkBox_temp;
    private boolean graphEnabled, useSeconds = false;
    private BroadcastReceiver dbChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadChargeCurve();
        }
    };

    public static void notify(Context context) {
        Intent intent = new Intent();
        intent.setAction(Contract.BROADCAST_STATUS_CHANGED);
        context.sendBroadcast(intent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        graph_chargeCurve = (GraphView) view.findViewById(R.id.graph_chargeCurve);
        viewport_chargeCurve = graph_chargeCurve.getViewport();
        textView_chargingTime = (TextView) view.findViewById(R.id.textView_chargingTime);
        graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), true);
        timeFormat = sharedPreferences.getString(getString(R.string.pref_time_format), "0");
        switch (timeFormat) {
            case "0":
                timeFormat = "%d h %.0f min";
                timeFormat_underHour = "%.0f min";
                timeFormat_underMinute = "%.0f min";
                break;
            case "1":
                timeFormat = "%d h %.1f min";
                timeFormat_underHour = "%.1f min";
                timeFormat_underMinute = "%.1f min";
                break;
            case "2":
                timeFormat = "%d h %.0f min %.0f s";
                timeFormat_underHour = "%.0f min %.0f s";
                timeFormat_underMinute = "%.0f s";
                useSeconds = true;
                break;
        }

        // checkBoxes
        checkBox_percentage = (CheckBox) view.findViewById(R.id.checkbox_percentage);
        checkBox_temp = (CheckBox) view.findViewById(R.id.checkBox_temp);

        if (!graphEnabled) { // disable checkboxes if the graph is disabled
            checkBox_percentage.setEnabled(false);
            checkBox_temp.setEnabled(false);
        } else {
            Context context = getContext();
            checkBox_percentage.setChecked(sharedPreferences.getBoolean(context.getString(R.string.pref_checkBox_percent), true));
            checkBox_temp.setChecked(sharedPreferences.getBoolean(context.getString(R.string.pref_checkBox_temperature), false));
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

        graph_chargeCurve.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) { // X-axis (time)
                    if (value == 0) {
                        graphCounter = 1;
                        return "0 min";
                    }
                    if (graphCounter++ % 3 == 0)
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
        filter.addAction(Contract.BROADCAST_ON_OFF_CHANGED);
        getActivity().registerReceiver(dbChangedReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Context context = getContext();
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_checkBox_percent), checkBox_percentage.isChecked())
                .putBoolean(context.getString(R.string.pref_checkBox_temperature), checkBox_temp.isChecked())
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
        // if graph disabled in settings -> return
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
        boolean isChargingAndNotFull = BatteryAlarmManager.isChargingNotificationEnabled(getContext(), sharedPreferences)
                && batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE) != 100
                && sharedPreferences.getBoolean(getContext().getString(R.string.pref_is_enabled), true);
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        LineGraphSeries<DataPoint> series[] = dbHelper.getGraphs();
        if (series != null) {
            series_chargeCurve = series[GraphDbHelper.TYPE_PERCENTAGE];
            series_temp = series[GraphDbHelper.TYPE_TEMPERATURE];
            setGraphColors();
            if (checkBox_percentage.isChecked()) {
                graph_chargeCurve.addSeries(series_chargeCurve);
            }
            if (checkBox_temp.isChecked()) {
                graph_chargeCurve.addSeries(series_temp);
            }
            double maxTime = series_chargeCurve.getHighestValueX();
            if (maxTime != 0) { // enough data
                if (isChargingAndNotFull) {
                    textView_chargingTime.setText(String.format("%s (%s)", getString(R.string.charging), getTimeString(maxTime)));
                } else {
                    textView_chargingTime.setText(String.format("%s: %s", getString(R.string.charging_time), getTimeString(maxTime)));
                }
                viewport_chargeCurve.setMaxX(maxTime);
            } else { // not enough data
                viewport_chargeCurve.setMaxX(1);
                if (isChargingAndNotFull) {
                    textView_chargingTime.setText(String.format("%s (%s)", getString(R.string.charging), getTimeString(0)));
                } else {
                    textView_chargingTime.setText(getString(R.string.not_enough_data));
                }
            }
        } else { // empty database
            if (isChargingAndNotFull) {
                textView_chargingTime.setText(String.format("%s (%s)", getTimeString(0), getString(R.string.charging)));
            } else {
                textView_chargingTime.setText(getString(R.string.no_data));
            }
        }
    }

    private String getTimeString(double timeInMinutes) { // returns "hours h minutes min" or "minutes min"
        if (timeInMinutes > 60) { // over an hour
            long hours = (long) timeInMinutes / 60;
            double minutes = (timeInMinutes - hours * 60);
            if (useSeconds) {
                double minutes_floor = Math.floor(minutes);
                double seconds = (minutes - minutes_floor) * 60;
                return String.format(Locale.getDefault(), timeFormat, hours, minutes_floor, seconds);
            }
            return String.format(Locale.getDefault(), timeFormat, hours, minutes);
        } else if (timeInMinutes > 1) { // under an hour, over a minute
            if (useSeconds) {
                double minutes = Math.floor(timeInMinutes);
                double seconds = (timeInMinutes - minutes) * 60;
                return String.format(Locale.getDefault(), timeFormat_underHour, minutes, seconds);
            }
            return String.format(Locale.getDefault(), timeFormat_underHour, timeInMinutes);
        } else { // under a minute
            if (useSeconds) {
                return String.format(Locale.getDefault(), timeFormat_underMinute, timeInMinutes * 60);
            }
            return String.format(Locale.getDefault(), timeFormat_underMinute, timeInMinutes);
        }
    }

    private void setGraphColors() {
        if (color_percentage == 0 || color_percentageBackground == 0 || color_temperature == 0) {
            // percentage
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            color_percentage = typedValue.data;
            color_percentageBackground = ColorUtils.setAlphaComponent(color_percentage, 64);
            // temperature
            if (sharedPreferences.getBoolean(getString(R.string.pref_dark_theme_enabled), false)) { // dark theme
                color_temperature = Color.GREEN;
            } else { // default theme
                theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                color_temperature = typedValue.data;
            }
        }
        series_chargeCurve.setColor(color_percentage);
        series_chargeCurve.setBackgroundColor(color_percentageBackground);
        series_temp.setColor(color_temperature);
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
}
