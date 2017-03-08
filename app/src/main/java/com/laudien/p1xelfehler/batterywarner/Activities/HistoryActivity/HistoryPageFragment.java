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
    protected LineGraphSeries<DataPoint>[] getSeries() {
        if (file != null) {
            GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
            return dbHelper.getGraphs(getContext(), dbHelper.getReadableDatabase(file.getPath()));
        } else {
            return null;
        }
    }

    /**
     * Returns the date the graph was created in milliseconds.
     * The difference between this method and the method in the super class is that if the time is
     * older than 1000000000 ms (from 1970), it returns the date the file was last modified.
     * This has been done because of back compatibility to older versions of the app where not the
     * real time but the time difference was saved.
     *
     * @return Returns the date the graph was created in milliseconds or the current time if no
     * file path was given in the arguments.
     */
    @Override
    protected long getCreationTime() {
        if (file != null) {
            GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
            long creationTime = GraphDbHelper.getEndTime(dbHelper.getReadableDatabase(file.getPath()));
            if (creationTime < 1000000000) {
                creationTime = file.lastModified();
            }
            return creationTime;
        }
        return Calendar.getInstance().getTimeInMillis();
    }

    /**
     * Deletes the file that was given by the arguments.
     * Shows a toast if the deletion failed.
     *
     * @return Returns true if the deletion was successful, false if not.
     */
    public boolean deleteFile() {
        boolean successful = file.delete();
        if (successful) {
            Toast.makeText(getContext(), getString(R.string.success_delete_graph), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
        }
        return successful;
    }

    /**
     * Returns the file path of the file that was given by the arguments.
     *
     * @return Returns the file path of the file that was given by the arguments.
     */
    public String getFileName() {
        return file.getName();
    }

    /**
     * Renames the file to the given name.
     *
     * @param newName The new name the file should be renamed to.
     * @return Returns true if the renaming was successful, false if not.
     */
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
