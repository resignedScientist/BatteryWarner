package com.laudien.p1xelfehler.batterywarner;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.RadioGroup;

import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.log.Lumberjack;

import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_CHARGING;

public class TaskerEditActivity extends AbstractAppCompatPluginActivity {
    RadioGroup radioGroup_action;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // apply the theme
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(getString(R.string.pref_dark_theme_enabled), getResources().getBoolean(R.bool.pref_dark_theme_enabled_default))) {
            setTheme(R.style.AppTheme_Dark);
        }

        setContentView(R.layout.activity_tasker_edit);
        radioGroup_action = findViewById(R.id.radio_group_action);

        // make a root check
        final Context context = getApplicationContext();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (!RootHelper.isRootAvailable()) {
                    NotificationHelper.showNotification(context, NotificationHelper.ID_NOT_ROOTED);
                }
            }
        });

        // configure the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        try {
            CharSequence title =
                    getPackageManager().getApplicationLabel(
                            getPackageManager().getApplicationInfo(getCallingPackage(),
                                    0));
            toolbar.setTitle(title);
        } catch (final PackageManager.NameNotFoundException e) {
            Lumberjack.e("Calling package couldn't be found%s", e); //$NON-NLS-1$
        }
        toolbar.setSubtitle(getString(R.string.tasker_plugin_name));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean isBundleValid(@NonNull Bundle bundle) {
        return TaskerHelper.isBundleValid(bundle);
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull Bundle bundle, @NonNull String s) {
        int action = TaskerHelper.getAction(bundle);
        Object value = TaskerHelper.getValue(bundle);
        switch (action) {
            case ACTION_TOGGLE_CHARGING:
                radioGroup_action.check(R.id.radioButton_toggle_charging);
                break;
        }
    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        int id = radioGroup_action.getCheckedRadioButtonId();
        switch (id) {
            case R.id.radioButton_toggle_charging:
                return TaskerHelper.buildBundle(ACTION_TOGGLE_CHARGING, true);
            default:
                return null;
        }
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull Bundle bundle) {
        int action = TaskerHelper.getAction(bundle);
        Object value = TaskerHelper.getValue(bundle);
        switch (action) {
            case ACTION_TOGGLE_CHARGING:
                return (Boolean) value ? getString(R.string.tasker_toggle_charging) : getString(R.string.tasker_disable_charging);
            default:
                return "Error!";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
