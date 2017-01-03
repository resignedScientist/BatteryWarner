package com.laudien.p1xelfehler.batterywarner.Activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.laudien.p1xelfehler.batterywarner.Adapter.ViewPagerAdapter;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.Fragments.GraphFragment;
import com.laudien.p1xelfehler.batterywarner.R;

public class MainActivity extends BaseActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
    private Toolbar toolbar;
    private ViewPagerAdapter viewPagerAdapter;
    private boolean backPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isAppInstalled();

        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (Contract.IS_PRO) {
            toolbar.setTitle(getString(R.string.app_name) + " Pro");
        } else {
            toolbar.setTitle(getString(R.string.app_name));
        }
        setSupportActionBar(toolbar);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(this);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            if (Contract.IS_PRO) {
                if (getSharedPreferences(Contract.SHARED_PREFS, MODE_PRIVATE)
                        .getBoolean(Contract.PREF_GRAPH_ENABLED, true)) {
                    GraphFragment graphFragment = (GraphFragment) viewPagerAdapter.getItem(1);
                    graphFragment.reloadChargeCurve();
                    Toast.makeText(getApplicationContext(), getString(R.string.graph_reloaded), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.disabled_in_settings), Toast.LENGTH_SHORT).show();
                }
                return true;
            } else {
                Toast.makeText(getApplicationContext(), "Sorry! :(", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        toolbar.getMenu().clear();
        switch (position) {
            case 1:
                toolbar.inflateMenu(R.menu.reload_menu);
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onBackPressed() {
        if (!backPressed) {
            Toast.makeText(getApplicationContext(), getString(R.string.click_to_exit), Toast.LENGTH_SHORT).show();
            backPressed = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    backPressed = false;
                }
            }, 3000);
        } else {
            finish();
        }
    }

    private void isAppInstalled() {
        // checks if 2 versions (free + pro) are installed and tells you that you have to uninstall the free one
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo(Contract.PACKAGE_NAME_FREE, PackageManager.GET_ACTIVITIES);
            packageManager.getPackageInfo(Contract.PACKAGE_NAME_PRO, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            return; // one of the app is not installed
        }
        // both apps are installed:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.uninstall_title))
                .setMessage(getString(R.string.uninstall_text))
                .setNegativeButton(getString(R.string.uninstall_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setPositiveButton(getString(R.string.uninstall_go), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Uri uri = Uri.parse("package:" + Contract.PACKAGE_NAME_FREE);
                        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
                        startActivity(uninstallIntent);
                    }
                })
                .show();
    }
}
