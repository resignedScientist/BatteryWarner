package com.laudien.p1xelfehler.batterywarner.receivers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseController;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper;
import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;

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
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.ACTION_CHANGE_PREFERENCE;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.ACTION_DISABLE_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.ACTION_ENABLE_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.EXTRA_PREFERENCE_KEY;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.EXTRA_PREFERENCE_VALUE;

public class TaskerFireReceiver extends AbstractPluginSettingReceiver {

    @Override
    protected boolean isBundleValid(@NonNull Context context, @NonNull Bundle bundle) {
        boolean valid = TaskerHelper.isBundleValid(context, bundle);
        if (!valid) {
            Toast.makeText(context, R.string.toast_invalid_tasker_settings, Toast.LENGTH_SHORT).show();
        }
        return valid;
    }

    @Override
    protected boolean isAsync() {
        return false;
    }

    @Override
    protected void firePluginSetting(@NonNull final Context context, @NonNull Bundle bundle) {
        Log.d(getClass().getSimpleName(), "Tasker Plugin fired!");
        if (bundle.containsKey(ACTION_TOGGLE_CHARGING))
            toggleCharging(context, bundle.getBoolean(ACTION_TOGGLE_CHARGING));
        if (bundle.containsKey(ACTION_TOGGLE_STOP_CHARGING))
            changePreference(context, R.string.pref_stop_charging, bundle.getBoolean(ACTION_TOGGLE_STOP_CHARGING));
        if (bundle.containsKey(ACTION_TOGGLE_SMART_CHARGING))
            changePreference(context, R.string.pref_smart_charging_enabled, bundle.getBoolean(ACTION_TOGGLE_SMART_CHARGING));
        if (bundle.containsKey(ACTION_TOGGLE_WARNING_HIGH))
            changePreference(context, R.string.pref_warning_high_enabled, bundle.getBoolean(ACTION_TOGGLE_WARNING_HIGH));
        if (bundle.containsKey(ACTION_TOGGLE_WARNING_LOW))
            changePreference(context, R.string.pref_warning_low_enabled, bundle.getBoolean(ACTION_TOGGLE_WARNING_LOW));
        if (bundle.containsKey(ACTION_SET_WARNING_HIGH))
            changePreference(context, R.string.pref_warning_high, bundle.getInt(ACTION_SET_WARNING_HIGH));
        if (bundle.containsKey(ACTION_SET_WARNING_LOW))
            changePreference(context, R.string.pref_warning_low, bundle.getInt(ACTION_SET_WARNING_LOW));
        if (bundle.containsKey(ACTION_SET_SMART_CHARGING_LIMIT))
            changePreference(context, R.string.pref_smart_charging_limit, bundle.getInt(ACTION_SET_SMART_CHARGING_LIMIT));
        if (bundle.containsKey(ACTION_SET_SMART_CHARGING_TIME))
            changePreference(context, R.string.pref_smart_charging_time, bundle.getLong(ACTION_SET_SMART_CHARGING_TIME));
        if (bundle.containsKey(ACTION_SAVE_GRAPH))
            saveGraph(context);
        if (bundle.containsKey(ACTION_RESET_GRAPH))
            resetGraph(context);
    }

    private void toggleCharging(Context context, boolean enabled) {
        Intent intent = new Intent(context.getApplicationContext(), BackgroundService.class);
        intent.setAction(enabled ? ACTION_ENABLE_CHARGING : ACTION_DISABLE_CHARGING);
        ServiceHelper.startService(context.getApplicationContext(), intent);
    }

    private void saveGraph(Context context) {
        DatabaseController databaseController = DatabaseController.getInstance(context);
        if (databaseController.saveGraph(context)) {
            Toast.makeText(context, R.string.toast_success_saving, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.toast_error_saving, Toast.LENGTH_SHORT).show();
        }
    }

    private void resetGraph(Context context) {
        DatabaseController databaseController = DatabaseController.getInstance(context);
        databaseController.resetTable();
        Toast.makeText(context, R.string.toast_success_delete_graph, Toast.LENGTH_SHORT).show();
    }

    private void changePreference(Context context, int keyResource, int value) {
        Intent intent = new Intent(context.getApplicationContext(), BackgroundService.class);
        intent.setAction(ACTION_CHANGE_PREFERENCE);
        intent.putExtra(EXTRA_PREFERENCE_KEY, keyResource);
        intent.putExtra(EXTRA_PREFERENCE_VALUE, value);
        ServiceHelper.startService(context, intent);
    }

    private void changePreference(Context context, int keyResource, long value) {
        Intent intent = new Intent(context.getApplicationContext(), BackgroundService.class);
        intent.setAction(ACTION_CHANGE_PREFERENCE);
        intent.putExtra(EXTRA_PREFERENCE_KEY, keyResource);
        intent.putExtra(EXTRA_PREFERENCE_VALUE, value);
        ServiceHelper.startService(context, intent);
    }

    private void changePreference(Context context, int keyResource, boolean value) {
        Intent intent = new Intent(context.getApplicationContext(), BackgroundService.class);
        intent.setAction(ACTION_CHANGE_PREFERENCE);
        intent.putExtra(EXTRA_PREFERENCE_KEY, keyResource);
        intent.putExtra(EXTRA_PREFERENCE_VALUE, value);
        ServiceHelper.startService(context, intent);
    }
}
