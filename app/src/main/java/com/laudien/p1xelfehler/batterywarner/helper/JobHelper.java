package com.laudien.p1xelfehler.batterywarner.helper;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.services.GraphAutoDeleteService;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@RequiresApi(LOLLIPOP)
public class JobHelper {
    public static final int ID_AUTO_DELETE_GRAPHS = 1337;
    private static final String TAG = JobHelper.class.getSimpleName();

    public static void schedule(Context context, int jobId) {
        if (jobId == ID_AUTO_DELETE_GRAPHS) {
            scheduleGraphAutoDelete(context);
        }
    }

    public static void cancel(Context context, int jobId) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(jobId);
        Log.d(TAG, "Job canceled: " + jobId);
    }

    private static void scheduleGraphAutoDelete(Context context) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(context, GraphAutoDeleteService.class);
            JobInfo jobInfo =
                    new JobInfo.Builder(ID_AUTO_DELETE_GRAPHS, componentName)
                            .setRequiresCharging(true)
                            .setPeriodic(1000 * 60 * 60 * 24)
                            .setPersisted(true) // survive reboots
                            .build();
            boolean result = jobScheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS;
            Log.d(TAG, "Job scheduled: " + ID_AUTO_DELETE_GRAPHS + ", success: " + result);
        } else {
            Log.d(TAG, "Scheduling job failed! No storage permission granted!");
        }
    }
}
