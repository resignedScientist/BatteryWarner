package com.laudien.p1xelfehler.batterywarner.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;

/**
 * Fragment that loads a charging curve from a file path given in the arguments.
 * Provides some functionality to change or remove the file.
 */
public class HistoryPageFragment extends BasicGraphFragment {
    /**
     * The key for the file path in the argument bundle.
     */
    public static final String EXTRA_FILE_PATH = "filePath";
    private File file;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            if (bundle.containsKey(EXTRA_FILE_PATH)) {
                String filePath = bundle.getString(EXTRA_FILE_PATH);
                if (filePath != null) {
                    file = new File(filePath);
                }
            }
        }
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
    protected LineGraphSeries[] getGraphs() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            if (file != null && file.exists()) {
                LineGraphSeries[] graphs = databaseController.getAllGraphs(file);
                styleGraphs(graphs);
                return graphs;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected long getEndTime() {
        return databaseController.getEndTime(file);
    }

    @Override
    protected long getStartTime() {
        return databaseController.getStartTime(file);
    }

    @Override
    protected void notifyTransitionsFinished() {
        databaseController.notifyTransitionsFinished(file);
    }
}
