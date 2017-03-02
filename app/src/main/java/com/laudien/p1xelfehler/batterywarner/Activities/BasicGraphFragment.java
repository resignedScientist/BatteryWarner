package com.laudien.p1xelfehler.batterywarner.Activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.laudien.p1xelfehler.batterywarner.R;

import java.util.Locale;

import static com.laudien.p1xelfehler.batterywarner.GraphDbHelper.TYPE_PERCENTAGE;
import static com.laudien.p1xelfehler.batterywarner.GraphDbHelper.TYPE_TEMPERATURE;

public abstract class BasicGraphFragment extends Fragment {

    protected final String TAG = getClass().getSimpleName();
    protected InfoObject infoObject;
    protected GraphView graphView;
    protected CheckBox checkBox_percentage, checkBox_temp;
    protected TextView textView_title, textView_chargingTime;
    protected LineGraphSeries<DataPoint>[] series;
    protected CompoundButton.OnCheckedChangeListener onCheckBoxChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            Series s = null;
            if (compoundButton == checkBox_percentage) {
                if (series != null) {
                    s = series[TYPE_PERCENTAGE];
                }
            } else if (compoundButton == checkBox_temp) {
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
    private int graphCounter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        graphView = (GraphView) view.findViewById(R.id.graphView);
        checkBox_percentage = (CheckBox) view.findViewById(R.id.checkbox_percentage);
        checkBox_percentage.setOnCheckedChangeListener(onCheckBoxChangeListener);
        checkBox_temp = (CheckBox) view.findViewById(R.id.checkBox_temp);
        checkBox_temp.setOnCheckedChangeListener(onCheckBoxChangeListener);
        textView_title = (TextView) view.findViewById(R.id.textView_title);
        textView_chargingTime = (TextView) view.findViewById(R.id.textView_chargingTime);
        initGraphView();
        graphView.getGridLabelRenderer().setLabelFormatter(getLabelFormatter());
        loadSeries();
        return view;
    }

    protected abstract LineGraphSeries<DataPoint>[] getSeries();

    protected abstract long getEndDate();

    protected void loadSeries() {
        series = getSeries();
        if (series != null) {
            if (checkBox_percentage.isChecked()) {
                graphView.addSeries(series[TYPE_PERCENTAGE]);
            }
            if (checkBox_temp.isChecked()) {
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

    protected void createOrUpdateInfoObject() {
        long endDate = getEndDate();
        if (infoObject == null) {
            infoObject = new InfoObject(
                    endDate,
                    series[TYPE_PERCENTAGE].getHighestValueX(),
                    series[TYPE_TEMPERATURE].getHighestValueY(),
                    series[TYPE_TEMPERATURE].getLowestValueY(),
                    series[TYPE_PERCENTAGE].getHighestValueY() - series[TYPE_PERCENTAGE].getLowestValueY()
            );
        } else {
            infoObject.updateValues(
                    endDate,
                    series[TYPE_PERCENTAGE].getHighestValueX(),
                    series[TYPE_TEMPERATURE].getHighestValueY(),
                    series[TYPE_TEMPERATURE].getLowestValueY(),
                    series[TYPE_PERCENTAGE].getHighestValueY() - series[TYPE_PERCENTAGE].getLowestValueY()
            );
        }
    }

    protected void setTimeText() {
        if (infoObject != null) {
            textView_chargingTime.setText(String.format(
                    Locale.getDefault(),
                    "%s: %s",
                    getString(R.string.charging_time),
                    infoObject.getTimeString(getContext())
            ));
        }
    }

    protected void reload() {
        Log.d(TAG, "reload()");
        graphView.removeAllSeries();
        loadSeries();
    }

    protected LabelFormatter getLabelFormatter() {
        return new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) { // X-axis (time)
                    if (value == 0) {
                        graphCounter = 1;
                        return "0 min";
                    }
                    if (value < 0.1) {
                        return "";
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
        };
    }

    protected void initGraphView() {
        Viewport viewport = graphView.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxY(100);
        viewport.setMinY(0);
    }

    public void showInfo() {
        if (series != null && infoObject != null) {
            infoObject.showDialog(getContext());
        } else {
            Toast.makeText(getContext(), getString(R.string.no_data), Toast.LENGTH_SHORT).show();
        }
    }
}
