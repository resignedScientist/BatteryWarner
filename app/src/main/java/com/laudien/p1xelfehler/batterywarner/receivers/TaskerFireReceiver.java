package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseController;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper;
import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_RESET_GRAPH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SAVE_GRAPH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_SMART_CHARGING_LIMIT;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_SMART_CHARGING_TIME;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_WARNING_HIGH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_WARNING_LOW;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_SMART_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_STOP_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_WARNING_HIGH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_WARNING_LOW;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.ACTION_DISABLE_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.ACTION_ENABLE_CHARGING;

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
        int action = TaskerHelper.getAction(bundle);
        Object value = TaskerHelper.getValue(bundle);
        Log.d(getClass().getSimpleName(), "Tasker Plugin fired! Action: " + TaskerHelper.getResultBlurb(context, bundle));
        try {
            switch (action) {
                case ACTION_TOGGLE_CHARGING:
                    toggleCharging(context, (Boolean) value);
                    break;
                case ACTION_TOGGLE_STOP_CHARGING:
                    changePreference(context, context.getString(R.string.pref_stop_charging), value);
                    break;
                case ACTION_TOGGLE_SMART_CHARGING:
                    changePreference(context, context.getString(R.string.pref_smart_charging_enabled), value);
                    break;
                case ACTION_TOGGLE_WARNING_HIGH:
                    changePreference(context, context.getString(R.string.pref_warning_high_enabled), value);
                    break;
                case ACTION_TOGGLE_WARNING_LOW:
                    changePreference(context, context.getString(R.string.pref_warning_low_enabled), value);
                    break;
                case ACTION_SET_WARNING_HIGH:
                    changePreference(context, context.getString(R.string.pref_warning_high), value);
                    break;
                case ACTION_SET_WARNING_LOW:
                    changePreference(context, context.getString(R.string.pref_warning_low), value);
                    break;
                case ACTION_SET_SMART_CHARGING_LIMIT:
                    changePreference(context, context.getString(R.string.pref_smart_charging_limit), value);
                    break;
                case ACTION_SET_SMART_CHARGING_TIME:
                    changePreference(context, context.getString(R.string.pref_smart_charging_time), value);
                    break;
                case ACTION_SAVE_GRAPH:
                    saveGraph(context);
                    break;
                case ACTION_RESET_GRAPH:
                    resetGraph(context);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleCharging(Context context, boolean enabled) {
        Intent intent = new Intent(context.getApplicationContext(), BackgroundService.class);
        intent.setAction(enabled ? ACTION_ENABLE_CHARGING : ACTION_DISABLE_CHARGING);
        ServiceHelper.startService(context.getApplicationContext(), intent);
    }

    private void saveGraph(Context context) {
        DatabaseController databaseController = DatabaseController.getInstance(context);
        databaseController.saveGraph(context);
    }

    private void resetGraph(Context context) {
        DatabaseController databaseController = DatabaseController.getInstance(context);
        databaseController.resetTable();
    }

    private void changePreference(Context context, String key, Object value) {
        try {
            Context myContext = context.createPackageContext(context.getString(R.string.package_name), 0);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            }
            editor.apply();
        } catch (PackageManager.NameNotFoundException ignored) { // cannot happen!
        }
    }
}
