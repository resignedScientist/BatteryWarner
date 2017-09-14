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
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseController;
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
    protected CompoundButton[] switches = new CompoundButton[NUMBER_OF_GRAPHS];
    /**
     * An instance of the {@link GraphInfo} holding information about the charging curve.
     */
    protected GraphInfo graphInfo;
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
    protected DatabaseController databaseController;
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
                            applyGraphScale();
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseController = DatabaseController.getInstance(getContext());
    }

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
        for (CompoundButton s : switches) {
            s.setOnCheckedChangeListener(onSwitchChangedListener);
        }
        textView_title = view.findViewById(R.id.textView_title);
        textView_chargingTime = view.findViewById(R.id.textView_chargingTime);
        initGraphView();
        graphView.getGridLabelRenderer().setLabelFormatter(getLabelFormatter());
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadSeries();
    }

    @Override
    public void onStop() {
        super.onStop();
        graphView.removeAllSeries();
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
            applyGraphScale();
            createOrUpdateInfoObject();
            enableOrDisableSwitches();
        }
        setTimeText();
        notifyTransitionsFinished();
    }

    protected void notifyTransitionsFinished() {
        databaseController.notifyTransitionsFinished();
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
            for (CompoundButton s : switches) {
                s.setEnabled(false);
            }
        }
    }

    /**
     * Dynamically scales the axis of the graphView.
     */
    protected void applyGraphScale() {
        double maxX = 0;
        double maxY = 0;
        double minY = 0;
        for (Series graph : graphView.getSeries()) {
            if (graph != null) {
                if (graph.getHighestValueX() > maxX) {
                    maxX = graph.getHighestValueX();
                }
                if (graph.getHighestValueY() > maxY) {
                    maxY = graph.getHighestValueY() * 1.2;
                }
                if (graph.getLowestValueY() < minY) {
                    minY = graph.getLowestValueY();
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
        if (maxY <= 100 * 1.2 && switches[GRAPH_INDEX_BATTERY_LEVEL].isChecked()) {
            maxY = 100;
        }
        Viewport viewport = graphView.getViewport();
        viewport.setMaxX(maxX);
        viewport.setMaxY(maxY);
        viewport.setMinY(minY);
    }

    /**
     * Creates a new or updates the existing instance of the
     * {@link GraphInfo}.
     */
    protected void createOrUpdateInfoObject() {
        if (graphs != null) {
            if (graphInfo == null) {
                graphInfo = new GraphInfo(
                        getContext(),
                        getStartTime(),
                        getEndTime(),
                        graphs[GRAPH_INDEX_BATTERY_LEVEL] != null ? graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueX() : Double.NaN,
                        graphs[GRAPH_INDEX_TEMPERATURE] != null ? graphs[GRAPH_INDEX_TEMPERATURE].getHighestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_TEMPERATURE] != null ? graphs[GRAPH_INDEX_TEMPERATURE].getLowestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_BATTERY_LEVEL] != null ? graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY() - graphs[GRAPH_INDEX_BATTERY_LEVEL].getLowestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_CURRENT] != null ? graphs[GRAPH_INDEX_CURRENT].getLowestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_CURRENT] != null ? graphs[GRAPH_INDEX_CURRENT].getHighestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_VOLTAGE] != null ? graphs[GRAPH_INDEX_VOLTAGE].getHighestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_VOLTAGE] != null ? graphs[GRAPH_INDEX_VOLTAGE].getLowestValueY() : Double.NaN
                );
            } else {
                graphInfo.updateValues(
                        getContext(),
                        getStartTime(),
                        getEndTime(),
                        graphs[GRAPH_INDEX_BATTERY_LEVEL] != null ? graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueX() : Double.NaN,
                        graphs[GRAPH_INDEX_TEMPERATURE] != null ? graphs[GRAPH_INDEX_TEMPERATURE].getHighestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_TEMPERATURE] != null ? graphs[GRAPH_INDEX_TEMPERATURE].getLowestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_BATTERY_LEVEL] != null ? graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueY() - graphs[GRAPH_INDEX_BATTERY_LEVEL].getLowestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_CURRENT] != null ? graphs[GRAPH_INDEX_CURRENT].getLowestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_CURRENT] != null ? graphs[GRAPH_INDEX_CURRENT].getHighestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_VOLTAGE] != null ? graphs[GRAPH_INDEX_VOLTAGE].getHighestValueY() : Double.NaN,
                        graphs[GRAPH_INDEX_VOLTAGE] != null ? graphs[GRAPH_INDEX_VOLTAGE].getLowestValueY() : Double.NaN
                );
            }
        } else { // there are no graphs
            graphInfo = null;
        }
    }

    /**
     * Sets the text of
     * {@link com.laudien.p1xelfehler.batterywarner.fragments.BasicGraphFragment#textView_chargingTime}
     * to the charging time.
     */
    void setTimeText() {
        if (graphInfo != null) {
            textView_chargingTime.setText(String.format(
                    Locale.getDefault(),
                    "%s: %s",
                    getString(R.string.info_charging_time),
                    graphInfo.getTimeString(getContext())
            ));
        }
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
     * Shows the info dialog defined in the {@link com.laudien.p1xelfehler.batterywarner.fragments.BasicGraphFragment#graphInfo}.
     * Shows a toast if there are no graphs or if the
     * {@link com.laudien.p1xelfehler.batterywarner.fragments.BasicGraphFragment#graphInfo} is null.
     */
    public void showInfo() {
        if (graphs != null && graphInfo != null) {
            graphInfo.showDialog(getContext());
        } else {
            ToastHelper.sendToast(getContext(), R.string.toast_no_data, LENGTH_SHORT);
        }
    }

    protected void styleGraphs(LineGraphSeries[] graphs) {
        if (graphs != null) {
            int[] colors = new int[NUMBER_OF_GRAPHS];
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
            colors[GRAPH_INDEX_BATTERY_LEVEL] = typedValue.data;
            int color_percentageBackground = ColorUtils.setAlphaComponent(colors[GRAPH_INDEX_BATTERY_LEVEL], 64);
            colors[GRAPH_INDEX_VOLTAGE] = Color.argb(255, 255, 165, 0);
            colors[GRAPH_INDEX_TEMPERATURE] = Color.argb(255, 104, 159, 56);
            colors[GRAPH_INDEX_CURRENT] = Color.argb(255, 63, 81, 181);
            // set colors
            for (byte i = 0; i < NUMBER_OF_GRAPHS; i++) {
                if (graphs[i] != null) {
                    graphs[i].setColor(colors[i]);
                    switches[i].setTextColor(colors[i]);
                    if (i == GRAPH_INDEX_BATTERY_LEVEL){
                        graphs[i].setDrawBackground(true);
                        graphs[i].setBackgroundColor(color_percentageBackground);
                    }
                }
            }
        }
    }
}
