package com.laudien.p1xelfehler.batterywarner.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;

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
        // resume charging using Background service
        Intent backgroundServiceIntent = new Intent(getApplicationContext(), BackgroundService.class);
        backgroundServiceIntent.setAction(BackgroundService.ACTION_ENABLE_CHARGING);
        ServiceHelper.startService(getApplicationContext(), backgroundServiceIntent);
    }
}
