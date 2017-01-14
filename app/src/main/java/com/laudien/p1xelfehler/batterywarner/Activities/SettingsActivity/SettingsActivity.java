package com.laudien.p1xelfehler.batterywarner.Activities.SettingsActivity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.laudien.p1xelfehler.batterywarner.Activities.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.R;

public class SettingsActivity extends BaseActivity {
    //private SettingsFragment settingsFragment;
    private NewSettingsFragment settingsFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.settings));
        try { // put version code in subtitle of the toolbar
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            toolbar.setSubtitle(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        setSupportActionBar(toolbar);

        //settingsFragment = new SettingsFragment();
        settingsFragment = new NewSettingsFragment();
        getFragmentManager().beginTransaction().replace(R.id.container_layout, settingsFragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.settings));
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        onNavigateUp();
    }
}
