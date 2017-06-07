package com.laudien.p1xelfehler.batterywarner.fragments.history;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.fragments.BasicGraphFragment;
import com.laudien.p1xelfehler.batterywarner.helper.GraphDbHelper;

import java.io.File;

/**
 * Fragment that loads a charging curve from a file path given in the arguments.
 * Provides some functionality to change or remove the file.
 */
public class HistoryPageFragment extends BasicGraphFragment {

    /**
     * The key for the file path in the argument bundle.
     */
    public static String EXTRA_FILE_PATH = "filePath";
    private File file;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle != null) {
            if (bundle.containsKey(EXTRA_FILE_PATH)) {
                String filePath = bundle.getString(EXTRA_FILE_PATH);
                if (filePath != null) {
                    file = new File(filePath);
                }
            }
        }
        Log.d(TAG, "fragment created with file: " + file.getPath());
        View view = super.onCreateView(inflater, container, savedInstanceState);
        textView_title.setVisibility(View.GONE);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_FILE_PATH)) {
                String filePath = savedInstanceState.getString(EXTRA_FILE_PATH);
                if (filePath != null && !filePath.equals("")) {
                    file = new File(filePath);
                }
            }
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_FILE_PATH, file.getPath());
    }

    /**
     * Loads the graphs out of the database from that the file path was given in the arguments.
     *
     * @return Returns an array of the graphs in the database or null if there was no file path
     * given in the arguments.
     */
    @Override
    protected LineGraphSeries<DataPoint>[] getSeries() {
        if (file != null && file.exists()) {
            GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
            return dbHelper.getGraphs(getContext(), dbHelper.getReadableDatabase(file.getPath()));
        } else {
            return null;
        }
    }

    @Override
    protected long getEndTime() {
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        return dbHelper.getEndTime(dbHelper.getReadableDatabase(file.getPath()));
    }

    @Override
    protected long getStartTime() {
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        return dbHelper.getStartTime(dbHelper.getReadableDatabase(file.getPath()));
    }

    /**
     * Returns the database file connected to this fragment instance.
     *
     * @return The database file where the graphs are saved.
     */
    public File getFile() {
        return file;
    }
}
