package com.laudien.p1xelfehler.batterywarner.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;

import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper.ID_STOP_CHARGING_NOT_WORKING;

/**
 * An IntentService called by the app that enables the charging again.
 * If the device is not rooted anymore, the notification with the id
 * ID_NOT_ROOTED will be triggered.
 * It stops itself after it finished (like every IntentService does!).
 */
public class EnableChargingService extends IntentService {
    public static final String ACTION_ENABLE_USB_CHARGING = "enableUsbCharging";
    public EnableChargingService() {
        super(null);
    }
    public EnableChargingService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_ENABLE_USB_CHARGING)) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            sharedPreferences.edit()
                    .putBoolean(getString(R.string.pref_usb_charging_disabled), false)
                    .apply();
        }
        try {
            RootHelper.enableCharging();
            NotificationHelper.cancelNotification(getApplicationContext(), BackgroundService.NOTIFICATION_ID_WARNING);
        } catch (RootHelper.NotRootedException e) {
            NotificationHelper.showNotification(EnableChargingService.this, ID_NOT_ROOTED);
            e.printStackTrace();
        } catch (RootHelper.NoBatteryFileFoundException e) {
            e.printStackTrace();
            NotificationHelper.showNotification(this, ID_STOP_CHARGING_NOT_WORKING);
        }
    }
}
