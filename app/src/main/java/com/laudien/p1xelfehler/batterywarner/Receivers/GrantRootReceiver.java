package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.RootChecker;

public class GrantRootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                return RootChecker.isDeviceRooted();
            }

            @Override
            protected void onPostExecute(Boolean rooted) {
                super.onPostExecute(rooted);
                if (rooted) {
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                            .putBoolean(context.getString(R.string.pref_stop_charging), true).apply();
                } else {
                    context.sendBroadcast(new Intent(context, DisableRootFeaturesReceiver.class));
                }
            }
        }.execute();
    }
}
