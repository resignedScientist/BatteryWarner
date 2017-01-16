package com.laudien.p1xelfehler.batterywarner.Activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(getString(R.string.pref_dark_theme_enabled), false)) {
            setTheme(R.style.DarkTheme);
        }
    }

    protected void setToolbarTitle() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (Contract.IS_PRO) {
            toolbar.setTitle(getString(R.string.app_name) + " Pro");
        } else {
            toolbar.setTitle(getString(R.string.app_name));
        }
        setSupportActionBar(toolbar);
    }

    protected void setToolbarTitle(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && getParentActivityIntent() != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        return super.onCreateOptionsMenu(menu);
    }
}
