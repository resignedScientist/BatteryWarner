package com.laudien.p1xelfehler.batterywarner.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;

import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_STOP_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_STOP_CHARGING_NOT_WORKING;

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
        try {
            RootHelper.enableCharging();
            NotificationHelper.cancelNotification(EnableChargingService.this, ID_STOP_CHARGING);
        } catch (RootHelper.NotRootedException e) {
            NotificationHelper.showNotification(EnableChargingService.this, ID_NOT_ROOTED);
            e.printStackTrace();
        } catch (RootHelper.NoBatteryFileFoundException e) {
            e.printStackTrace();
            NotificationHelper.showNotification(this, ID_STOP_CHARGING_NOT_WORKING);
        }
    }
}
