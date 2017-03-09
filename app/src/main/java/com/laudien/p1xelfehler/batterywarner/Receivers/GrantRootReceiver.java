package com.laudien.p1xelfehler.batterywarner.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.NotificationBuilder;
import com.laudien.p1xelfehler.batterywarner.RootChecker;
import com.laudien.p1xelfehler.batterywarner.Services.DisableRootFeaturesService;

import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.NotificationBuilder.ID_STOP_CHARGING;

/**
 * A BroadcastReceiver called by the app.
 * It asks for root permissions again and triggers the correct notifications depending on the
 * state in the battery file (which enables/disables the charging) and the battery state.
 * If the user does not grant the root permission, it calls the DisableRootFeaturesService.
 */
public class GrantRootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                return RootChecker.isRootAvailable();
            }

            @Override
            protected void onPostExecute(Boolean rooted) {
                super.onPostExecute(rooted);
                if (rooted) { // rooting was allowed now
                    Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    if (batteryStatus != null) {
                        boolean isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
                        if (!isCharging) { // if not charging make sure that it is not disabled by the app
                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        boolean chargingEnabled = RootChecker.isChargingEnabled();
                                        if (!chargingEnabled) { // if disabled by app, show notification!
                                            NotificationBuilder.showNotification(context, ID_STOP_CHARGING);
                                        }
                                    } catch (RootChecker.NotRootedException e) { // user disabled root again after allowing it
                                        e.printStackTrace();
                                        NotificationBuilder.showNotification(context, ID_NOT_ROOTED);
                                    } catch (RootChecker.BatteryFileNotFoundException e) {
                                        // Should not happen! Is checked before the user can enable the feature!
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                } else { // user is stupid and keeps root disabled -> disable all root features
                    context.startService(new Intent(context, DisableRootFeaturesService.class));
                }
            }
        }.execute();
    }
}
