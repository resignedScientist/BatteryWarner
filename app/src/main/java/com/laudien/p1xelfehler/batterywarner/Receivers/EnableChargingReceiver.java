package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.RootChecker;

import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_NOT_ROOTED;

/**
 * A BroadcastReceiver called by the app that enables the charging again.
 * If the device is not rooted anymore, the notification with the id
 * ID_NOT_ROOTED will be triggered.
 */
public class EnableChargingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RootChecker.enableCharging(context);
                } catch (RootChecker.NotRootedException e) {
                    NotificationBuilder.showNotification(context, ID_NOT_ROOTED);
                    e.printStackTrace();
                }
            }
        });
    }
}
