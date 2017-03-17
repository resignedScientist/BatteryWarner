package com.laudien.p1xelfehler.batterywarner.Activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.R;

/**
 * Super class for all activities in the app. It applies the theme,
 * initializes the toolbar and sets its title.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected Toast toast;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(getString(R.string.pref_dark_theme_enabled), getResources().getBoolean(R.bool.pref_dark_theme_enabled_default))) {
            setTheme(R.style.DarkTheme);
        }
    }

    /**
     * Sets the toolbar title to the proper app name depending on if it is the pro version or not.
     */
    protected void setToolbarTitle() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (Contract.IS_PRO) {
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

    public void showToast(int messageResource, int length) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this, messageResource, length);
        toast.show();
    }

    public void showToast(String message, int length) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(this, message, length);
        toast.show();
    }
}
