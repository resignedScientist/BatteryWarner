package com.laudien.p1xelfehler.batterywarner.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseController;

import java.io.File;
import java.util.Calendar;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class GraphAutoDeleteService extends JobService {
    private boolean stopped = false;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
        boolean autoDeleteEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_auto_delete), getResources().getBoolean(R.bool.pref_graph_auto_delete_default));
        if (graphEnabled && autoDeleteEnabled) {
            stopped = false;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    doTheJob(sharedPreferences, jobParameters);
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

    private void doTheJob(SharedPreferences sharedPreferences, JobParameters jobParameters) {
        int days = sharedPreferences.getInt(getString(R.string.pref_graph_auto_delete_time), getResources().getInteger(R.integer.pref_graph_auto_delete_time_default));
        // calculate the time - everything older than this time will be deleted
        long daysInMillis = days * 24 * 60 * 60 * 1000;
        long targetTime = SystemClock.currentThreadTimeMillis() - daysInMillis;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(targetTime);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long targetTimeFlattened = calendar.getTimeInMillis();
        // delete every file older than the target time
        File directory = new File(DatabaseController.DATABASE_HISTORY_PATH);
        File[] files = directory.listFiles();
        for (File file : files) {
            if (stopped) {
                return;
            }
            if (file.lastModified() < targetTimeFlattened) {
                file.delete();
            }
        }
        jobFinished(jobParameters, true);
        // TODO: Do this with the DatabaseController
        // TODO: Add onGraphDeleted() callback to the DatabaseController
    }
}
