package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper;
import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;
import com.laudien.p1xelfehler.batterywarner.services.EnableChargingService;
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

public class TaskerFireReceiver extends AbstractPluginSettingReceiver {

    @Override
    protected boolean isBundleValid(@NonNull Bundle bundle) {
        return TaskerHelper.isBundleValid(bundle);
    }

    @Override
    protected boolean isAsync() {
        return false;
    }

    @Override
    protected void firePluginSetting(@NonNull final Context context, @NonNull Bundle bundle) {
        final boolean charging = TaskerHelper.getBundleResult(bundle);
        Log.d(getClass().getSimpleName(), "Tasker Plugin fired! Charging = " + charging);

        if (charging) { // charging should be enabled
            // do the same as the "Enable charging" button on the stop charging notification
            Intent intent = new Intent(context.getApplicationContext(), EnableChargingService.class);
            ServiceHelper.startService(context.getApplicationContext(), intent);
        } else { // charging should be disabled
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        RootHelper.disableCharging();
                        // notify the background service
                        Intent intent = new Intent(context.getApplicationContext(), BackgroundService.class);
                        intent.setAction(BackgroundService.ACTION_CHARGING_DISABLED);
                        ServiceHelper.startService(context.getApplicationContext(), intent);
                    } catch (RootHelper.NotRootedException e) {
                        e.printStackTrace();
                        NotificationHelper.showNotification(context.getApplicationContext(), NotificationHelper.ID_NOT_ROOTED);
                    } catch (RootHelper.NoBatteryFileFoundException e) {
                        e.printStackTrace();
                        NotificationHelper.showNotification(context.getApplicationContext(), NotificationHelper.ID_STOP_CHARGING_NOT_WORKING);
                    }
                }
            });
        }
    }
}
