package com.laudien.p1xelfehler.batterywarner.Activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import java.util.Locale;

import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.GraphDbHelper.TYPE_PERCENTAGE;
import static com.laudien.p1xelfehler.batterywarner.HelperClasses.GraphDbHelper.TYPE_TEMPERATURE;

/**
 * Super class of all Fragments that are using the charging curve.
 */
public abstract class BasicGraphFragment extends Fragment {

    /**
     * The Tag for logging purposes
     */
    protected final String TAG = getClass().getSimpleName();
    /**
     * An instance of the InfoObject holding information about the charging curve.
     */
    protected InfoObject infoObject;
    /**
     * The GraphView where the graphs are shown
     */
    protected GraphView graphView;
    /**
     * Checkbox which turns the percentage graph on and off.
     */
    protected Switch switch_percentage;
    /**
     * Checkbox which turns the temperature graph on and off.
     */
    protected Switch switch_temp;
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
    protected LineGraphSeries<DataPoint>[] series;
    private byte labelCounter;
    private CompoundButton.OnCheckedChangeListener onSwitchChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            Series s = null;
            if (compoundButton == switch_percentage) {
                if (series != null) {
                    s = series[TYPE_PERCENTAGE];
                }
            } else if (compoundButton == switch_temp) {
                if (series != null) {
                    s = series[TYPE_TEMPERATURE];
                }
            }
            if (s != null) {
                if (checked) {
                    graphView.addSeries(s);
                } else {
                    graphView.removeSeries(s);
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        graphView = (GraphView) view.findViewById(R.id.graphView);
        switch_percentage = (Switch) view.findViewById(R.id.switch_percentage);
        switch_percentage.setOnCheckedChangeListener(onSwitchChangedListener);
        switch_temp = (Switch) view.findViewById(R.id.switch_temp);
        switch_temp.setOnCheckedChangeListener(onSwitchChangedListener);
        textView_title = (TextView) view.findViewById(R.id.textView_title);
        textView_chargingTime = (TextView) view.findViewById(R.id.textView_chargingTime);
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
    protected abstract LineGraphSeries<DataPoint>[] getSeries();

    /**
     * Method that provides the time the graph was created.
     *
     * @return Returns time the graph was created in milliseconds.
     */
    protected abstract long getCreationTime();

    /**
     * Method that loads the graph into the GraphView and sets the text of the TextView that show the time.
     * You can override it to only do it under some conditions (for example only allow it for the pro version).
     */
    protected void loadSeries() {
        series = getSeries();
        if (series != null) {
            if (switch_percentage.isChecked()) {
                graphView.addSeries(series[TYPE_PERCENTAGE]);
            }
            if (switch_temp.isChecked()) {
                graphView.addSeries(series[TYPE_TEMPERATURE]);
            }
            createOrUpdateInfoObject();
            double highestValue = series[TYPE_PERCENTAGE].getHighestValueX();
            if (highestValue > 0) {
                graphView.getViewport().setMaxX(highestValue);
            } else {
                graphView.getViewport().setMaxX(1);
            }
        } else {
            graphView.getViewport().setMaxX(1);
        }
        setTimeText();
    }

    /**
     * Creates a new or updates the existing instance of the InfoObject that is used to store
     * information about the graphs.
     */
    protected void createOrUpdateInfoObject() {
        long creationTime = getCreationTime();
        if (infoObject == null) {
            infoObject = new InfoObject(
                    creationTime,
                    series[TYPE_PERCENTAGE].getHighestValueX(),
                    series[TYPE_TEMPERATURE].getHighestValueY(),
                    series[TYPE_TEMPERATURE].getLowestValueY(),
                    series[TYPE_PERCENTAGE].getHighestValueY() - series[TYPE_PERCENTAGE].getLowestValueY()
            );
        } else {
            infoObject.updateValues(
                    creationTime,
                    series[TYPE_PERCENTAGE].getHighestValueX(),
                    series[TYPE_TEMPERATURE].getHighestValueY(),
                    series[TYPE_TEMPERATURE].getLowestValueY(),
                    series[TYPE_PERCENTAGE].getHighestValueY() - series[TYPE_PERCENTAGE].getLowestValueY()
            );
        }
    }

    /**
     * Sets the text of the textView_chargingTime TextView to the charging time.
     */
    protected void setTimeText() {
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
    protected void reload() {
        graphView.removeAllSeries();
        loadSeries();
    }

    /**
     * Provides the format of the text of the x and y axis of the graph.
     * @return Returns a LabelFormatter that is used in the GraphView.
     */
    protected LabelFormatter getLabelFormatter() {
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
                } else if (switch_percentage.isChecked() ^ switch_temp.isChecked()) { // Y-axis (percent)
                    if (switch_percentage.isChecked())
                        return super.formatLabel(value, false) + "%";
                    if (switch_temp.isChecked())
                        return super.formatLabel(value, false) + "°C";
                }
                return super.formatLabel(value, false);
            }
        };
    }

    /**
     * Initializes the ViewPort of the GraphView. Sets the part of the x and y axis that is shown.
     */
    protected void initGraphView() {
        Viewport viewport = graphView.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxY(100);
        viewport.setMinY(0);
    }

    /**
     * Shows the info dialog defined in the InfoObject. Shows a toast if there are no graphs or
     * if there is no InfoObject.
     */
    public void showInfo() {
        if (series != null && infoObject != null) {
            infoObject.showDialog(getContext());
        } else {
            ((BaseActivity) getActivity()).showToast(R.string.toast_no_data, LENGTH_SHORT);
        }
    }
}
