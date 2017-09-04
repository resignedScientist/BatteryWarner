package com.laudien.p1xelfehler.batterywarner.fragments;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import java.util.Locale;

import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.GRAPH_INDEX_VOLTAGE;

/**
 * Super class of all Fragments that are using the charging curve.
 */
public abstract class BasicGraphFragment extends Fragment {
    private static final double HIGHEST_X_DEFAULT = 1;
    private static final double HIGHEST_Y_DEFAULT = 100;
    /**
     * An instance of the {@link com.laudien.p1xelfehler.batterywarner.fragments.InfoObject} holding information about the charging curve.
     */
    protected InfoObject infoObject;
    /**
     * The GraphView where the graphs are shown
     */
    protected GraphView graphView;
    /**
     * Switch which turns the percentage graph on and off.
     */
    protected Switch switch_percentage;
    /**
     * Switch which turns the temperature graph on and off.
     */
    protected Switch switch_temp;
    /**
     * Switch which turns the current graph on and off.
     */
    protected Switch switch_current;
    /**
     * Switch which turns the voltage graph on and off.
     */
    protected Switch switch_voltage;
    /**
     * TextView that contains the title over the GraphView.
     */
    protected TextView textView_title;
    /**
     * TextView that contains the charging time.
     */
    protected TextView textView_chargingTime;
    /**
     * An array of both graphs that are displayed in the GraphView.
     */
    LineGraphSeries<DataPoint>[] graphs;

    /**
     * An {@link android.widget.CompoundButton.OnCheckedChangeListener} managing all switches.
     */
    private final CompoundButton.OnCheckedChangeListener onSwitchChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            Series s = null;
            if (graphs != null) {
                if (compoundButton == switch_percentage)
                    s = graphs[GRAPH_INDEX_BATTERY_LEVEL];
                else if (compoundButton == switch_temp)
                    s = graphs[GRAPH_INDEX_TEMPERATURE];
                else if (compoundButton == switch_current)
                    s = graphs[GRAPH_INDEX_CURRENT];
                else if (compoundButton == switch_voltage)
                    s = graphs[GRAPH_INDEX_VOLTAGE];
            }
            if (s != null) {
                if (checked) {
                    graphView.addSeries(s);
                } else {
                    graphView.removeSeries(s);
                }
                applyMaxValues();
            }
        }
    };
    /**
     * A byte containing the number of graph labels.
     */
    private byte labelCounter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        graphView = view.findViewById(R.id.graphView);
        switch_percentage = view.findViewById(R.id.switch_percentage);
        switch_percentage.setOnCheckedChangeListener(onSwitchChangedListener);
        switch_temp = view.findViewById(R.id.switch_temp);
        switch_temp.setOnCheckedChangeListener(onSwitchChangedListener);
        switch_current = view.findViewById(R.id.switch_current);
        switch_current.setOnCheckedChangeListener(onSwitchChangedListener);
        switch_voltage = view.findViewById(R.id.switch_voltage);
        switch_voltage.setOnCheckedChangeListener(onSwitchChangedListener);
        textView_title = view.findViewById(R.id.textView_title);
        textView_chargingTime = view.findViewById(R.id.textView_chargingTime);
        initGraphView();
        graphView.getGridLabelRenderer().setLabelFormatter(getLabelFormatter());
        loadSeries();
        return view;
    }

    /**
     * Method that provides an array of the graphs that should be displayed.
     *
     * @return Returns an array of graphs.
     */
    protected abstract LineGraphSeries<DataPoint>[] getGraphs();

    /**
     * Method that provides the time the graph was created.
     *
     * @return Returns time the graph was created in milliseconds.
     */
    protected abstract long getEndTime();

    protected abstract long getStartTime();

    /**
     * Method that loads the graph into the GraphView and sets the text of the TextView that show the time.
     * You can override it to only do it under some conditions.
     */
    void loadSeries() {
        graphs = getGraphs();
        if (graphs != null) {
            if (switch_percentage.isChecked() && graphs[GRAPH_INDEX_BATTERY_LEVEL] != null) {
                graphView.addSeries(graphs[GRAPH_INDEX_BATTERY_LEVEL]);
            }
            if (switch_temp.isChecked() && graphs[GRAPH_INDEX_TEMPERATURE] != null) {
                graphView.addSeries(graphs[GRAPH_INDEX_TEMPERATURE]);
            }
            if (switch_current.isChecked() && graphs[GRAPH_INDEX_CURRENT] != null) {
                graphView.addSeries(graphs[GRAPH_INDEX_CURRENT]);
            }
            if (switch_voltage.isChecked() && graphs[GRAPH_INDEX_VOLTAGE] != null) {
                graphView.addSeries(graphs[GRAPH_INDEX_VOLTAGE]);
            }
            applyMaxValues();
            createOrUpdateInfoObject();
            disableNotSupportedSwitches();
        }
        setTimeText();
    }

    /**
     * Disables all switches that have no graph. This can be because the value is not supported on
     * the device or when using older database files.
     */
    private void disableNotSupportedSwitches() {
        if (graphs != null) {
            for (byte i = 0; i < graphs.length; i++) {
                if (graphs[i] == null) {
                    switch (i) {
                        case GRAPH_INDEX_BATTERY_LEVEL:
                            switch_percentage.setEnabled(false);
                            break;
                        case GRAPH_INDEX_TEMPERATURE:
                            switch_temp.setEnabled(false);
                            break;
                        case GRAPH_INDEX_CURRENT:
                            switch_current.setEnabled(false);
                            break;
                        case GRAPH_INDEX_VOLTAGE:
                            switch_voltage.setEnabled(false);
                            break;
                    }
                }
            }
        }
    }

    /**
     * Dynamically scales the axis of the graphView.
     */
    protected void applyMaxValues() {
        double maxX = 0;
        double maxY = 0;
        for (Series graph : graphView.getSeries()) {
            if (graph != null) {
                if (graph.getHighestValueX() > maxX) {
                    maxX = graph.getHighestValueX();
                }
                if (graph.getHighestValueY() > maxY) {
                    maxY = graph.getHighestValueY() * 1.2;
                }
            }
        }
        if (maxX == 0) {
            maxX = HIGHEST_X_DEFAULT;
            maxY = HIGHEST_Y_DEFAULT;
        }
        if (maxY == 0) {
            maxY = HIGHEST_Y_DEFAULT;
        }
        graphView.getViewport().setMaxX(maxX);
        graphView.getViewport().setMaxY(maxY);
    }

    /**
     * Creates a new or updates the existing instance of the
     * {@link com.laudien.p1xelfehler.batterywarner.fragments.InfoObject}.
     */
    private void createOrUpdateInfoObject() {
        if (graphs != null
                && graphs[GRAPH_INDEX_BATTERY_LEVEL] != null
                && graphs[GRAPH_INDEX_TEMPERATURE] != null) {
            if (infoObject == null) {
                infoObject = new InfoObject(
                        getStartTime(),
                        getEndTime(),
                        graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueX(),
                        graphs[GRAPH_INDEX_TEMPERATURE].getHighestValueY(),
                        graphs[GRAPH_INDEX_TEMPERATURE].getLowestValueY(),
                        graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY() - graphs[GRAPH_INDEX_BATTERY_LEVEL].getLowestValueY()
                );
            } else {
                infoObject.updateValues(
                        getStartTime(),
                        getEndTime(),
                        graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueX(),
                        graphs[GRAPH_INDEX_TEMPERATURE].getHighestValueY(),
                        graphs[GRAPH_INDEX_TEMPERATURE].getLowestValueY(),
                        graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY() - graphs[GRAPH_INDEX_BATTERY_LEVEL].getLowestValueY()
                );
            }
        } else { // any graph is null
            infoObject = null;
        }
    }

    /**
     * Sets the text of
     * {@link com.laudien.p1xelfehler.batterywarner.fragments.BasicGraphFragment#textView_chargingTime}
     * to the charging time.
     */
    void setTimeText() {
        if (infoObject != null) {
            textView_chargingTime.setText(String.format(
                    Locale.getDefault(),
                    "%s: %s",
                    getString(R.string.info_charging_time),
                    infoObject.getTimeString(getContext())
            ));
        }
    }

    /**
     * Reloads the graphs from the database.
     */
    void reload() {
        graphView.removeAllSeries();
        loadSeries();
    }

    /**
     * Provides the format of the text of the x and y axis of the graph.
     *
     * @return Returns a LabelFormatter that is used in the GraphView.
     */
    private LabelFormatter getLabelFormatter() {
        return new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) { // X-axis (time)
                    if (value == 0) {
                        labelCounter = 1;
                        return "0 min";
                    }
                    if (value < 0.1) {
                        return "";
                    }
                    if (labelCounter++ % 3 == 0)
                        return super.formatLabel(value, true) + " min";
                    return "";
                } else if (switch_percentage.isChecked() ^ switch_temp.isChecked() ^ switch_current.isChecked() ^ switch_voltage.isChecked()) { // Y-axis (percent)
                    String suffix = "";
                    if (switch_percentage.isChecked())
                        suffix = "%";
                    if (switch_temp.isChecked())
                        suffix = "°C";
                    if (switch_current.isChecked())
                        suffix = "mA";
                    if (switch_voltage.isChecked())
                        suffix = "V";
                    return super.formatLabel(value, false) + suffix;

                }
                return super.formatLabel(value, false);
            }
        };
    }

    /**
     * Initializes the ViewPort of the GraphView. Sets the part of the x and y axis that is shown.
     */
    private void initGraphView() {
        Viewport viewport = graphView.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(HIGHEST_X_DEFAULT);
        viewport.setMinY(0);
        viewport.setMaxY(HIGHEST_Y_DEFAULT);
    }

    /**
     * Shows the info dialog defined in the {@link com.laudien.p1xelfehler.batterywarner.fragments.BasicGraphFragment#infoObject}.
     * Shows a toast if there are no graphs or if the
     * {@link com.laudien.p1xelfehler.batterywarner.fragments.BasicGraphFragment#infoObject} is null.
     */
    public void showInfo() {
        if (graphs != null && infoObject != null) {
            infoObject.showDialog(getContext());
        } else {
            ToastHelper.sendToast(getContext(), R.string.toast_no_data, LENGTH_SHORT);
        }
    }

    protected void styleGraphs(LineGraphSeries[] graphs) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean darkThemeEnabled = sharedPreferences.getBoolean(getString(R.string.pref_dark_theme_enabled), getResources().getBoolean(R.bool.pref_dark_theme_enabled_default));
        // percentage
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        int color_percentage = typedValue.data;
        int color_percentageBackground = ColorUtils.setAlphaComponent(color_percentage, 64);
        // temperature
        int color_temperature;
        if (darkThemeEnabled) { // dark theme
            color_temperature = Color.GREEN;
        } else { // default theme
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
            color_temperature = typedValue.data;
        }
        graphs[GRAPH_INDEX_BATTERY_LEVEL].setDrawBackground(true);
        graphs[GRAPH_INDEX_BATTERY_LEVEL].setColor(color_percentage);
        graphs[GRAPH_INDEX_BATTERY_LEVEL].setBackgroundColor(color_percentageBackground);
        graphs[GRAPH_INDEX_TEMPERATURE].setColor(color_temperature);
    }
}
