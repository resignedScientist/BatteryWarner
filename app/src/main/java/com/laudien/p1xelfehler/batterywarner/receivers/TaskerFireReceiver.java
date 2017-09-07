package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper;
import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;
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
        Intent intent = new Intent(context.getApplicationContext(), BackgroundService.class);
        if (charging) { // charging should be enabled
            intent.setAction(BackgroundService.ACTION_ENABLE_CHARGING);
        } else { // charging should be disabled
            intent.setAction(BackgroundService.ACTION_DISABLE_CHARGING);
        }
        ServiceHelper.startService(context.getApplicationContext(), intent);
    }
}
