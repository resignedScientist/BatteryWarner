package com.laudien.p1xelfehler.batterywarner.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    private static final String Key_FILE_PATH = "filePath";
    public int index;
    public HistoryPageFragmentDataSource dataSource;
    private String filePath;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            filePath = savedInstanceState.getString(Key_FILE_PATH);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        textView_title.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history_page_menu, menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (filePath != null) {
            outState.putString(Key_FILE_PATH, filePath);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_info) {
            showInfo();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        if (dataSource == null && filePath == null) {
            return null;
        }
        if (dataSource == null) {
            return new File(filePath);
        }
        File file = dataSource.getFile(index);
        filePath = file != null ? file.getPath() : null;
        return dataSource.getFile(index);
    }
}

