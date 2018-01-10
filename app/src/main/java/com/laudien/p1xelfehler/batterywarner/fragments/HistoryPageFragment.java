package com.laudien.p1xelfehler.batterywarner.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.TemperatureConverter;

import java.io.File;

/**
 * Fragment that loads a charging curve from a file path given in the arguments.
 * Provides some functionality to change or remove the file.
 */
public class HistoryPageFragment extends BasicGraphFragment {
    public int index;
    public HistoryPageFragmentDataSource dataSource;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        textView_title.setVisibility(View.GONE);
        return view;
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
            File file = getFile();
            if (file != null && file.exists()) {
                boolean useFahrenheit = TemperatureConverter.useFahrenheit(getContext());
                boolean reverseCurrent = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(getString(R.string.pref_reverse_current), getResources().getBoolean(R.bool.pref_reverse_current_default));
                LineGraphSeries[] graphs = databaseController.getAllGraphs(file, useFahrenheit, reverseCurrent);
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
        File file = getFile();
        if (file == null) {
            return 0;
        }
        return databaseController.getEndTime(file);
    }

    @Override
    protected long getStartTime() {
        File file = getFile();
        if (file == null) {
            return 0;
        }
        return databaseController.getStartTime(file);
    }

    @Override
    protected void notifyTransitionsFinished() {
        File file = getFile();
        if (file != null) {
            databaseController.notifyTransactionsFinished(file);
        }
    }

    @Nullable
    private File getFile() {
        if (dataSource == null) {
            return null;
        }
        return dataSource.getFile(index);
    }
}

