package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;
import com.twofortyfouram.assertion.BundleAssertions;
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;
import com.twofortyfouram.log.Lumberjack;

public class TaskerFireReceiver extends AbstractPluginSettingReceiver {
    public static final String EXTRA_TOGGLE_CHARGING = "toggleCharging";

    @Override
    protected boolean isBundleValid(@NonNull Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        try {
            BundleAssertions.assertHasBoolean(bundle, EXTRA_TOGGLE_CHARGING);
        } catch (AssertionError e) {
            Lumberjack.e("Bundle failed verification%s", e);
            return false;
        }
        return true;
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
