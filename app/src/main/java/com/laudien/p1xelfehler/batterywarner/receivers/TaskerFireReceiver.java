package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;
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
    protected void firePluginSetting(@NonNull Context context, @NonNull Bundle bundle) {
        ToastHelper.sendToast(context, "Plugin fired! :)");
        Log.d(getClass().getSimpleName(), "Plugin fired! :)");
    }
}
