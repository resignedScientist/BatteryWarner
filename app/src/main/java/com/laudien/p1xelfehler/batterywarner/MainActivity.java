package com.laudien.p1xelfehler.batterywarner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.laudien.p1xelfehler.batterywarner.appIntro.IntroActivity;
import com.laudien.p1xelfehler.batterywarner.fragments.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.fragments.MainPageFragment;
import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ServiceHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * The main activity that is shown to the user after opening the app.
 * It will show the {@link com.laudien.p1xelfehler.batterywarner.appIntro.IntroActivity}
 * if it was not already finished.
 */
public class MainActivity extends BaseActivity {
    private boolean backPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.createNotificationChannels(this);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstStart = sharedPreferences.getBoolean(getString(R.string.pref_first_start), true);
        if (firstStart) {
            startActivity(new Intent(this, IntroActivity.class));
            finish();
        } else {
            setContentView(R.layout.activity_main);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            ViewPager viewPager = findViewById(R.id.viewPager);
            if (viewPager != null) { // phones only
                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
                viewPager.setAdapter(viewPagerAdapter);
                TabLayout tabLayout = findViewById(R.id.tab_layout);
                tabLayout.setupWithViewPager(viewPager);
            }
            // start services just in case
            ServiceHelper.startService(this);
        }
    }

    @Override
    public void onBackPressed() {
        if (!backPressed) {
            ToastHelper.sendToast(this, R.string.toast_click_to_exit, LENGTH_SHORT);
            backPressed = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    backPressed = false;
                }
            }, 3000);
        } else {
            finishAffinity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new MainPageFragment();
                case 1:
                    return new GraphFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_main_page);
                case 1:
                    return getString(R.string.title_stats);
                default:
                    return null;
            }
        }
    }
}
