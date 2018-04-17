package com.laudien.p1xelfehler.batterywarner.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.laudien.p1xelfehler.batterywarner.GraphLoader;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.Data;

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
        if (savedInstanceState != null) {
            filePath = savedInstanceState.getString(Key_FILE_PATH);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        textView_title.setVisibility(View.GONE);
        fetchGraphs();
        return view;
    }

    @Override
    protected void fetchGraphs() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final File file = getFile();
        if (file == null || !file.exists()) {
            return;
        }
        getLoaderManager().restartLoader(index, null, new LoaderManager.LoaderCallbacks<Data>() {
            @NonNull
            @Override
            public Loader<Data> onCreateLoader(int id, @Nullable Bundle args) {
                return new GraphLoader(getContext(), file);
            }

            @Override
            public void onLoadFinished(@NonNull Loader<Data> loader, @Nullable Data data) {
                if (data == null) {
                    return;
                }
                loadGraphs(data.getGraphs());
                graphInfo = data.getGraphInfo();
            }

            @Override
            public void onLoaderReset(@NonNull Loader<Data> loader) {

            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history_page_menu, menu);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
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

