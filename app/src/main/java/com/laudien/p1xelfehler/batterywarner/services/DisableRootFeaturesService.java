package com.laudien.p1xelfehler.batterywarner.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import static android.widget.Toast.LENGTH_LONG;

/**
 * A Service started by the app which disables all the root features.
 * It shows a toast to notify the user about it and stops itself after it finished.
 */
public class DisableRootFeaturesService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ToastHelper.sendToast(this, R.string.toast_root_denied, LENGTH_LONG);
        RootHelper.disableAllRootPreferences(this);
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
