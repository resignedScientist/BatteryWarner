package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.laudien.p1xelfehler.batterywarner.RootChecker;

public class NotificationDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RootChecker.enableCharging(context);
                } catch (RootChecker.NotRootedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
