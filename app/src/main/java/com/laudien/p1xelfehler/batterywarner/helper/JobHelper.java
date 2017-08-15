package com.laudien.p1xelfehler.batterywarner.helper;


import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.BuildConfig;
import com.laudien.p1xelfehler.batterywarner.R;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobHelper {
    private static final int ID_AUTO_DELETE_GRAPHS = 1337;
    private static final String PACKAGE_NAME_AUTO_DELETE_GRAPHS = BuildConfig.APPLICATION_ID.concat(".services");

    public static void scheduleJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int autoDeleteDays = sharedPreferences.getInt(context.getString(R.string.pref_graph_auto_delete_time), context.getResources().getInteger(R.integer.pref_graph_auto_delete_time_default));
        long autoDeleteTimeInMillis = autoDeleteDays * 24 * 60 * 60 * 1000;
        JobInfo jobInfo =
                new JobInfo.Builder(ID_AUTO_DELETE_GRAPHS, new ComponentName(PACKAGE_NAME_AUTO_DELETE_GRAPHS, "GraphAutoDeleteService.class"))
                        .setRequiresCharging(true)
                        .setPeriodic(autoDeleteTimeInMillis)
                        .build();
        String result = jobScheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS ? "success" : "failure";
        Log.d("JobHelper", "Job scheduled, result: " + result);
    }

    public static void cancelJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(ID_AUTO_DELETE_GRAPHS);
    }
}
