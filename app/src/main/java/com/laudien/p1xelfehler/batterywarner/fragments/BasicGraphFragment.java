package com.laudien.p1xelfehler.batterywarner.fragments;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
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
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.NUMBER_OF_GRAPHS;

/**
 * Super class of all Fragments that are using the charging curve.
 */
public abstract class BasicGraphFragment extends Fragment {
    private static final double HIGHEST_X_DEFAULT = 1;
    private static final double HIGHEST_Y_DEFAULT = 100;
    /**
     * Array of all switches.
     */
    protected Switch[] switches = new Switch[NUMBER_OF_GRAPHS];
    /**
     * An instance of the {@link com.laudien.p1xelfehler.batterywarner.fragments.InfoObject} holding information about the charging curve.
     */
    protected InfoObject infoObject;
    /**
     * The GraphView where the graphs are shown
     */
    protected GraphView graphView;
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
            if (graphs != null) {
                for (byte i = 0; i < switches.length; i++) { // iterate through every graph/switch id
                    if (switches[i] == compoundButton) { // the id of the changed switch was found
                        LineGraphSeries graph = graphs[i]; // get the graph with that id
                        if (graph != null) {
                            if (checked) {
                                graphView.addSeries(graph);
                            } else { // unchecked
                                graphView.removeSeries(graph);
                            }
                            applyMaxValues();
                        }
                        break;
                    }
                }
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
        switches[GRAPH_INDEX_BATTERY_LEVEL] = view.findViewById(R.id.switch_percentage);
        switches[GRAPH_INDEX_TEMPERATURE] = view.findViewById(R.id.switch_temp);
        switches[GRAPH_INDEX_CURRENT] = view.findViewById(R.id.switch_current);
        switches[GRAPH_INDEX_VOLTAGE] = view.findViewById(R.id.switch_voltage);
        // set the listener for each switch
        for (Switch s : switches) {
            s.setOnCheckedChangeListener(onSwitchChangedListener);
        }
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
            for (byte i = 0; i < NUMBER_OF_GRAPHS; i++) {
                if (switches[i].isChecked() && graphs[i] != null) {
                    graphView.addSeries(graphs[i]);
                }
            }
            applyMaxValues();
            createOrUpdateInfoObject();
            enableOrDisableSwitches();
        }
        setTimeText();
    }

    protected void enableOrDisableSwitches() {
        if (graphs != null) {
            for (byte i = 0; i < graphs.length; i++) {
                if (graphs[i] != null && graphs[i].getHighestValueX() > 0) {
                    switches[i].setEnabled(true);
                } else { // the graph with the id i is null or has not enough data points
                    switches[i].setEnabled(false);
                    if (graphs[i] == null) {
                        switches[i].setChecked(false);
                    }
                }
            }
        } else { // graph array is null
            for (Switch s : switches) {
                s.setEnabled(false);
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
    protected void createOrUpdateInfoObject() {
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
                } else { // Y-axis (percent)
                    byte checkedSwitches = 0;
                    byte checkedSwitchId = -1;
                    for (byte i = 0; i < switches.length; i++) {
                        if (switches[i].isChecked()) {
                            checkedSwitches++;
                            checkedSwitchId = i;
                        }
                    }
                    if (checkedSwitches == 1) { // only one switch is checked
                        String suffix;
                        switch (checkedSwitchId) {
                            case GRAPH_INDEX_BATTERY_LEVEL:
                                suffix = "%";
                                break;
                            case GRAPH_INDEX_TEMPERATURE:
                                suffix = "°C";
                                break;
                            case GRAPH_INDEX_CURRENT:
                                suffix = "mA";
                                break;
                            case GRAPH_INDEX_VOLTAGE:
                                suffix = "V";
                                break;
                            default:
                                suffix = "";
                        }
                        return super.formatLabel(value, false) + suffix;
                    } else {
                        return super.formatLabel(value, false);
                    }
                }
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
        int[] colors = new int[NUMBER_OF_GRAPHS];
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        colors[GRAPH_INDEX_BATTERY_LEVEL] = typedValue.data;
        int color_percentageBackground = ColorUtils.setAlphaComponent(colors[GRAPH_INDEX_BATTERY_LEVEL], 64);
        colors[GRAPH_INDEX_VOLTAGE] = Color.argb(255, 255, 165, 0);
        colors[GRAPH_INDEX_TEMPERATURE] = Color.GREEN;
        colors[GRAPH_INDEX_CURRENT] = Color.BLUE;
        // set colors
        for (byte i = 0; i < NUMBER_OF_GRAPHS; i++) {
            if (graphs[i] != null) {
                graphs[i].setColor(colors[i]);
            }
        }
        graphs[GRAPH_INDEX_BATTERY_LEVEL].setDrawBackground(true);
        graphs[GRAPH_INDEX_BATTERY_LEVEL].setBackgroundColor(color_percentageBackground);
    }
}
