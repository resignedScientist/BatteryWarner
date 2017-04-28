package com.laudien.p1xelfehler.batterywarner.Services;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.Helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.Helper.RootHelper;

import static com.laudien.p1xelfehler.batterywarner.Helper.NotificationHelper.ID_GRANT_ROOT;
import static com.laudien.p1xelfehler.batterywarner.Helper.NotificationHelper.ID_NOT_ROOTED;
import static com.laudien.p1xelfehler.batterywarner.Helper.NotificationHelper.ID_STOP_CHARGING;

/**
 * An IntentService called by the app.
 * It asks for root permissions again and triggers the correct notifications depending on the
 * state in the battery file (which enables/disables the charging) and the battery state.
 * If the user does not grant the root permission, it calls the DisableRootFeaturesService.
 * It stops itself after it finished (like every IntentService does!).
 */
public class GrantRootService extends IntentService {
    public GrantRootService() {
        super(null);
    }

    public GrantRootService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        boolean rooted = RootHelper.isRootAvailable();
        NotificationHelper.cancelNotification(this, ID_GRANT_ROOT, ID_NOT_ROOTED);
        if (rooted) { // rooting was allowed now
            Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryStatus != null) {
                boolean isCharging = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) != 0;
                if (!isCharging) { // if not charging make sure that it is not disabled by the app
                    try {
                        boolean chargingEnabled = RootHelper.isChargingEnabled();
                        if (!chargingEnabled) { // if disabled by app, show notification!
                            NotificationHelper.showNotification(GrantRootService.this, ID_STOP_CHARGING);
                        }
                    } catch (RootHelper.NotRootedException e) { // user disabled root again after allowing it
                        e.printStackTrace();
                        NotificationHelper.showNotification(GrantRootService.this, ID_NOT_ROOTED);
                    } catch (RootHelper.NoBatteryFileFoundException e) {
                        // Should not happen! Is checked before the user can enable the feature!
                        e.printStackTrace();
                    }
                }
            }
        } else { // user is stupid and keeps root disabled -> disable all root features
            startService(new Intent(GrantRootService.this, DisableRootFeaturesService.class));
        }
    }
}
