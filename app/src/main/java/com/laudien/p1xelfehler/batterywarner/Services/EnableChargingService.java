package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.RootChecker;

import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_STOP_CHARGING;

/**
 * An IntentService called by the app that enables the charging again.
 * If the device is not rooted anymore, the notification with the id
 * ID_NOT_ROOTED will be triggered.
 * It stops itself after it finished (like every IntentService does!).
 */
public class EnableChargingService extends IntentService {
    public EnableChargingService() {
        super(null);
    }
    public EnableChargingService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        stopService(new Intent(this, ChargingService.class));
        try {
            RootChecker.enableCharging(EnableChargingService.this);
            NotificationBuilder.cancelNotification(EnableChargingService.this, ID_STOP_CHARGING);
        } catch (RootChecker.NotRootedException e) {
            NotificationBuilder.showNotification(EnableChargingService.this, ID_NOT_ROOTED);
            e.printStackTrace();
        }
    }
}
