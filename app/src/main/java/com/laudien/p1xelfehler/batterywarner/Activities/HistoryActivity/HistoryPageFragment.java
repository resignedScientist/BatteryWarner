package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.Series;
import com.laudien.p1xelfehler.batterywarner.Activities.InfoObject;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;
import java.util.Locale;

import static com.laudien.p1xelfehler.batterywarner.GraphDbHelper.TYPE_PERCENTAGE;
import static com.laudien.p1xelfehler.batterywarner.GraphDbHelper.TYPE_TEMPERATURE;

public class HistoryPageFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "HistoryPageFragment";
    private int graphCounter;
    private File file;
    private InfoObject infoObject;
    private GraphView graphView;
    private CheckBox checkBox_percentage, checkBox_temp;
    private TextView textView_chargingTime;
    private Series[] series;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(false);
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        graphView = (GraphView) view.findViewById(R.id.graphView);
        TextView textView_title = (TextView) view.findViewById(R.id.textView_title);
        textView_title.setVisibility(View.GONE);
        textView_chargingTime = (TextView) view.findViewById(R.id.textView_chargingTime);
        checkBox_temp = (CheckBox) view.findViewById(R.id.checkBox_temp);
        checkBox_temp.setOnCheckedChangeListener(this);
        checkBox_percentage = (CheckBox) view.findViewById(R.id.checkbox_percentage);
        checkBox_percentage.setOnCheckedChangeListener(this);
        initGraphView();
        return view;
    }

    private void initGraphView() {
        if (file == null) {
            return;
        }
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        series = dbHelper.getGraphs(getContext(), dbHelper.getReadableDatabase(file.getPath()));
        if (series == null) {
            return;
        }
        for (Series s : series) {
            graphView.addSeries(s);
        }
        Viewport viewport = graphView.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMaxX(series[0].getHighestValueX());
        viewport.setMinX(0);
        viewport.setMaxY(100);
        viewport.setMinY(0);

        long endTime = GraphDbHelper.getEndTime(dbHelper.getReadableDatabase(file.getPath()));
        if (endTime < 1000000000) {
            endTime = file.lastModified();
        }

        infoObject = new InfoObject(
                endTime,
                series[0].getHighestValueX(),
                series[1].getHighestValueY(),
                series[1].getLowestValueY(),
                series[0].getHighestValueY() - series[0].getLowestValueY()
        );

        textView_chargingTime.setText(String.format(
                Locale.getDefault(),
                "%s: %s",
                getString(R.string.charging_time),
                infoObject.getTimeString(getContext())
        ));

        graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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
        });
    }

    public void addGraphsFromFile(File file) {
        this.file = file;
    }

    public boolean deleteFile() {
        boolean successful = file.delete();
        if (successful) {
            Toast.makeText(getContext(), getString(R.string.success_delete_graph), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
        }
        return successful;
    }

    public String getFileName() {
        return file.getName();
    }

    public boolean renameFile(String newName) {
        if (this.file.getName().equals(newName)) {
            return false;
        }
        File file = new File(Contract.DATABASE_HISTORY_PATH + "/" + newName);
        if (file.exists()) {
            Toast.makeText(getContext(), "There already is a graph named '" + newName + "'!", Toast.LENGTH_SHORT).show();
            return false;
        }
        boolean successful = this.file.renameTo(file);
        if (successful) {
            Toast.makeText(getContext(), getString(R.string.success_renaming), Toast.LENGTH_SHORT).show();
            this.file = file;
        } else {
            Toast.makeText(getContext(), getString(R.string.error_renaming), Toast.LENGTH_SHORT).show();
        }
        return successful;
    }

    public void showInfo() {
        if (infoObject != null) {
            infoObject.showDialog(getActivity());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        Series series = null;
        if (compoundButton == checkBox_percentage) {
            series = this.series[TYPE_PERCENTAGE];
        } else if (compoundButton == checkBox_temp) {
            series = this.series[TYPE_TEMPERATURE];
        } else {
            return;
        }
        if (checked) {
            graphView.addSeries(series);
        } else {
            graphView.removeSeries(series);
        }
    }
}
