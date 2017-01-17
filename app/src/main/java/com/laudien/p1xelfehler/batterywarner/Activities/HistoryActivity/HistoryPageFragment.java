package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.Series;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;

public class HistoryPageFragment extends Fragment {

    private static final String TAG = "HistoryPageFragment";
    private int graphCounter;
    private File file;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_page, container, false);
        GraphView graphView = (GraphView) view.findViewById(R.id.graphView);
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        Series[] series = dbHelper.getGraphs(getContext(), dbHelper.getReadableDatabase(file.getPath()));
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

        graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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
                }
                return super.formatLabel(value, false);
            }
        });

        return view;
    }

    public void addGraphsFromFile(File file) {
        this.file = file;
    }
}
