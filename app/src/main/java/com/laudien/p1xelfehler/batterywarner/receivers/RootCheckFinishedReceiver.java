package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.PACKAGE_NAME_FREE;

public abstract class RootCheckFinishedReceiver extends BroadcastReceiver {
    public static final String ACTION_ROOT_CHECK_FINISHED = PACKAGE_NAME_FREE + ".ROOT_CHECK_FINISHED";
    public static final String EXTRA_ROOT_ALLOWED = PACKAGE_NAME_FREE + ".ROOT_ALLOWED";
    public static final String EXTRA_PREFERENCE = PACKAGE_NAME_FREE + ".PREFERENCE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_ROOT_CHECK_FINISHED)) {
            if (intent.hasExtra(EXTRA_ROOT_ALLOWED) && intent.hasExtra(EXTRA_PREFERENCE)) {
                boolean rootAllowed = intent.getBooleanExtra(EXTRA_ROOT_ALLOWED, false);
                final String preferenceKey = intent.getStringExtra(EXTRA_PREFERENCE);
                if (!rootAllowed) { // root access was not granted
                    ToastHelper.sendToast(context, R.string.toast_not_rooted, LENGTH_SHORT);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            disablePreferences(preferenceKey);
                        }
                    }, context.getResources().getInteger(R.integer.pref_switch_back_delay));
                }
            } else {
                throw new RuntimeException("The Intent does not contain all extras!");
            }
        }
    }

    protected abstract void disablePreferences(String preferenceKey);
}
