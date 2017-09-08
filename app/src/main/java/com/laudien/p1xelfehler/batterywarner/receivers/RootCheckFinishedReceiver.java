package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * Abstract BroadcastReceiver that will be called after a check for root finished.
 * This class is only used in PreferenceFragments that contain root preferences.
 */
public abstract class RootCheckFinishedReceiver extends BroadcastReceiver {
    public static final String ACTION_ROOT_CHECK_FINISHED = "com.laudien.p1xelfehler.batterywarner.ROOT_CHECK_FINISHED";
    /**
     * Required extra that contains a boolean. True = root was allowed; False = root was not allowed.
     */
    public static final String EXTRA_ROOT_ALLOWED = "com.laudien.p1xelfehler.batterywarner.ROOT_ALLOWED";
    /**
     * Required extra that contains the preference key where root was asked for as String.
     */
    public static final String EXTRA_PREFERENCE = "com.laudien.p1xelfehler.batterywarner.PREFERENCE";

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
                } else { // root access was granted
                    ServiceHelper.restartService(context.getApplicationContext());
                }
            } else {
                throw new RuntimeException("The Intent does not contain all extras!");
            }
        }
    }

    /**
     * If root was not allowed, this method will be called for the preference key where root
     * was asked for. The preference should be disabled here.
     *
     * @param preferenceKey The key that was given in EXTRA_PREFERENCE.
     */
    protected abstract void disablePreferences(String preferenceKey);
}
