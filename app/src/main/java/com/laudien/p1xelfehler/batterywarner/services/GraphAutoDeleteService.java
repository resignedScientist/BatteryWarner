package com.laudien.p1xelfehler.batterywarner.services;

import android.Manifest;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils;

import java.io.File;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class GraphAutoDeleteService extends JobService {
    private static final String TAG = GraphAutoDeleteService.class.getSimpleName();
    private boolean canceled = false;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                deleteOldFiles(jobParameters);
            }
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        canceled = true;
        return true;
    }

    private void deleteOldFiles(JobParameters jobParameters) {
        Log.d(TAG, "Job started!");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
        boolean autoDeleteEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_auto_delete), getResources().getBoolean(R.bool.pref_graph_auto_delete_default));
        if (!graphEnabled || !autoDeleteEnabled) { // disabled in settings
            Log.d(TAG, "Auto delete is disabled in settings!");
            return;
        }
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) { // storage permission not granted
            Log.d(TAG, "Deletion failed! No storage permission granted!");
            return;
        }
        int days = sharedPreferences.getInt(getString(R.string.pref_graph_auto_delete_time), getResources().getInteger(R.integer.pref_graph_auto_delete_time_default));
        long deletionTime = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * days); // everything older will be deleted
        ArrayList<File> files = DatabaseUtils.getBatteryFiles();
        for (File file : files) {
            if (file.lastModified() > deletionTime) { // last modified time newer than deletion time
                continue;
            }
            if (canceled) {
                canceled = false;
                Log.d(TAG, "Job canceled!");
                return;
            }
            if (file.delete()) {
                Log.d(TAG, "File deleted: " + file.getPath());
            } else {
                Log.d(TAG, "Deletion failed! File: " + file.getPath());
            }
        }
        Log.d(TAG, "Job finished!");
        jobFinished(jobParameters, false);
    }
}
