package com.laudien.p1xelfehler.batterywarner.helper;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.support.annotation.RequiresApi;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

@RequiresApi(LOLLIPOP)
public class JobHelper {
    private static final int JOB_ID_GRAPH_AUTO_DELETE = 1337;

    public void schedule (int jobId){
        if (jobId == JOB_ID_GRAPH_AUTO_DELETE){
            scheduleGraphAutoDelete();
        }
    }

    private void scheduleGraphAutoDelete(){
        
    }
}
