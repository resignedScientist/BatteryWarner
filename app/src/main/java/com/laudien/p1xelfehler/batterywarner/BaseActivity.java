package com.laudien.p1xelfehler.batterywarner;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

/**
 * Super class for all activities in the app. It applies the theme,
 * initializes the toolbar and sets its title.
 */
public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(getString(R.string.pref_dark_theme_enabled), getResources().getBoolean(R.bool.pref_dark_theme_enabled_default))) {
            setTheme(R.style.AppTheme_Dark);
        }
    }

    /**
     * Sets the toolbar title to the proper app name depending on if it is the pro version or not.
     */
    protected void setToolbarTitle() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (AppInfoHelper.IS_PRO) {
            toolbar.setTitle(getString(R.string.app_name) + " Pro");
        } else {
            toolbar.setTitle(getString(R.string.app_name));
        }
        setSupportActionBar(toolbar);
    }

    /**
     * Method to easily change the title of the toolbar.
     *
     * @param title Title to apply to the toolbar.
     */
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
