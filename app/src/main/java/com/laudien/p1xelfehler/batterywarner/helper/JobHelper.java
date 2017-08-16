package com.laudien.p1xelfehler.batterywarner.helper;


import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.services.GraphAutoDeleteService;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobHelper {
    private static final int ID_AUTO_DELETE_GRAPHS = 1337;
    private static final String TAG = JobHelper.class.getSimpleName();

    public static void scheduleJob(Context context) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            ComponentName componentName = new ComponentName(context, GraphAutoDeleteService.class);
            JobInfo jobInfo =
                    new JobInfo.Builder(ID_AUTO_DELETE_GRAPHS, componentName)
                            .setRequiresCharging(true)
                            .setPeriodic(24 * 60 * 60 * 1000)
                            .setPersisted(true) // survive reboots
                            .build();
            boolean result = jobScheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS;
            Log.d(TAG, "Job scheduled, success: " + result);
        } else {
            Log.d(TAG, "Scheduling job failed! No storage permission granted!");
        }
    }

    public static void cancelJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(ID_AUTO_DELETE_GRAPHS);
        Log.d(TAG, "Job canceled!");
    }
}
