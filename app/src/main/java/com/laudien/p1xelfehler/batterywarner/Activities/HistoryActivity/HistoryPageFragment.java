package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.Series;
import com.laudien.p1xelfehler.batterywarner.Activities.DialogManager;
import com.laudien.p1xelfehler.batterywarner.Activities.InfoObject;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;
import java.util.List;

public class HistoryPageFragment extends Fragment {

    private static final String TAG = "HistoryPageFragment";
    private int graphCounter;
    private File file;
    private GraphView graphView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_page, container, false);
        graphView = (GraphView) view.findViewById(R.id.graphView);
        initGraphView();
        return view;
    }

    private void initGraphView() {
        if (file == null) {
            return;
        }
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
        List<Series> series = graphView.getSeries();
        DialogManager.getInstance().showInfoDialog(getActivity(), new InfoObject(
                series.get(0).getHighestValueX(),
                series.get(1).getHighestValueY(),
                series.get(1).getLowestValueY()
        ));
    }
}
