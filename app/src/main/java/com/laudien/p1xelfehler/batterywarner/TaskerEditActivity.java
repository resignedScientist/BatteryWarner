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

        /*
         * To help the user keep context, the title shows the host's name and the subtitle
         * shows the plug-in's name.
         */
        /*CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel =
                    getPackageManager().getApplicationLabel(
                            getPackageManager().getApplicationInfo(getCallingPackage(),
                                    0));
        } catch (final PackageManager.NameNotFoundException e) {
            Lumberjack.e("Calling package couldn't be found%s", e); //$NON-NLS-1$
        }
        if (null != callingApplicationLabel) {
            setTitle(callingApplicationLabel);
        }

        getSupportActionBar().setSubtitle("Neat battery warner plugin");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/
    }

    @Override
    public boolean isBundleValid(@NonNull Bundle bundle) {
        return TaskerHelper.isBundleValid(bundle);
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull Bundle bundle, @NonNull String s) {
        boolean charging = TaskerHelper.getBundleResult(bundle);
        radioGroup_action.check(charging ? R.id.radioButton_enable_charging : R.id.radioButton_disable_charging);
    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        boolean charging = radioGroup_action.getCheckedRadioButtonId() == R.id.radioButton_enable_charging;
        return TaskerHelper.buildBundle(charging);
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull Bundle bundle) {
        boolean result = TaskerHelper.getBundleResult(bundle);
        return result ? getString(R.string.tasker_enable_charging) : getString(R.string.tasker_disable_charging);
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
