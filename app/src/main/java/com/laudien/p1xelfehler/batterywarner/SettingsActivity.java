package com.laudien.p1xelfehler.batterywarner;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.laudien.p1xelfehler.batterywarner.preferences.SettingsFragment;

/**
 * An Activity that is the frame for the SettingsFragment. It shows the version name of the app
 * in the toolbar subtitle.
 */
public class SettingsActivity extends BaseActivity {

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preference_base_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.title_preferences));
        try { // put version code in subtitle of the toolbar
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            toolbar.setSubtitle(String.format("%s (%d)", pInfo.versionName, pInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        setSupportActionBar(toolbar);

        // replace container layout with SettingsFragment
        getFragmentManager().beginTransaction().replace(R.id.container_layout, new SettingsFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        onNavigateUp();
    }
}
