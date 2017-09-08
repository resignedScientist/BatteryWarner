package com.laudien.p1xelfehler.batterywarner.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;

/**
 * An IntentService called by the notification that enables the charging again.
 * If the device is not rooted anymore, the notification with the id
 * ID_NOT_ROOTED will be triggered.
 * It stops itself after it finished (like every IntentService does!).
 */
public class ResumeChargingButtonService extends IntentService {
    /**
     * 'Enable Usb Charging' clicked.
     */
    public static final String ACTION_ENABLE_USB_CHARGING = "enableUsbCharging";
    /**
     * 'Enable Charging' clicked. This will save the graph.
     */
    public static final String ACTION_RESUME_CHARGING_SAVE_GRAPH = "resumeChargingSaveGraph";
    /**
     * 'Enable Usb Charging' clicked if charging is not allowed. The BackgroundService will not
     * save the graph then.
     */
    public static final String ACTION_RESUME_CHARGING_NOT_SAVE_GRAPH = "resumeChargingNotSaveGraph";

    public ResumeChargingButtonService() {
        super(null);
    }

    public ResumeChargingButtonService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Intent backgroundServiceIntent = new Intent(getApplicationContext(), BackgroundService.class);
            switch (intent.getAction()) {
                case ACTION_ENABLE_USB_CHARGING: // 'Enable USB charging' button
                    // change 'USB charging disabled' to false
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    sharedPreferences.edit()
                            .putBoolean(getString(R.string.pref_usb_charging_disabled), false)
                            .apply();
                case ACTION_RESUME_CHARGING_NOT_SAVE_GRAPH: // 'Enable charging' button if the notification was called because of not allowed charging
                    backgroundServiceIntent.setAction(BackgroundService.ACTION_ENABLE_CHARGING);
                    break;
                case ACTION_RESUME_CHARGING_SAVE_GRAPH: // normal 'Enable charging' button
                    backgroundServiceIntent.setAction(BackgroundService.ACTION_ENABLE_CHARGING_AND_SAVE_GRAPH);
                    break;
                default:
                    throw new RuntimeException("Unknown action!");
            }
            // resume charging using Background service
            ServiceHelper.startService(getApplicationContext(), backgroundServiceIntent);
        }
    }
}
