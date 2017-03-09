package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.RootChecker;

import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_NOT_ROOTED;

/**
 * A Service called by the app that enables the charging again.
 * If the device is not rooted anymore, the notification with the id
 * ID_NOT_ROOTED will be triggered.
 * It stops itself after it finished.
 */
public class EnableChargingService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RootChecker.enableCharging(EnableChargingService.this);
                } catch (RootChecker.NotRootedException e) {
                    NotificationBuilder.showNotification(EnableChargingService.this, ID_NOT_ROOTED);
                    e.printStackTrace();
                }
            }
        });
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
