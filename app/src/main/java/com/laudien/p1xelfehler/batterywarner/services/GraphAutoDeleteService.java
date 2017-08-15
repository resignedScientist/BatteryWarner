package com.laudien.p1xelfehler.batterywarner.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseController;

import java.io.File;
import java.util.Collection;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class GraphAutoDeleteService extends JobService {
    private boolean stopped = false;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
        boolean autoDeleteEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_auto_delete), getResources().getBoolean(R.bool.pref_graph_auto_delete_default));
        if (graphEnabled && autoDeleteEnabled) {
            stopped = false;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    doTheJob(jobParameters);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        stopped = true;
        return true;
    }

    private void doTheJob(JobParameters jobParameters) {
        DatabaseController databaseController = DatabaseController.getInstance(getApplicationContext());
        Collection<File> oldFiles = databaseController.getOldGraphFiles(getApplicationContext());
        for (File file : oldFiles) {
            if (stopped) {
                return;
            }
            if (file.delete()) {
                databaseController.notifyGraphFileDeleted(file);
            }
        }
        jobFinished(jobParameters, true);
    }
}
