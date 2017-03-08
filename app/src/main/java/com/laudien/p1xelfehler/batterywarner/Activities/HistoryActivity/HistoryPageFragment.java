package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.Activities.BasicGraphFragment;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import java.io.File;
import java.util.Calendar;

/**
 *
 */
public class HistoryPageFragment extends BasicGraphFragment {

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
        View view = super.onCreateView(inflater, container, savedInstanceState);
        textView_title.setVisibility(View.GONE);
        return view;
    }

    @Override
    protected LineGraphSeries<DataPoint>[] getSeries() {
        if (file != null) {
            GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
            return dbHelper.getGraphs(getContext(), dbHelper.getReadableDatabase(file.getPath()));
        } else {
            return null;
        }
    }

    @Override
    protected long getCreationTime() {
        if (file != null) {
            GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
            long endDate = GraphDbHelper.getEndTime(dbHelper.getReadableDatabase(file.getPath()));
            if (endDate < 1000000000) {
                endDate = file.lastModified();
            }
            return endDate;
        }
        return Calendar.getInstance().getTimeInMillis();
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
}
