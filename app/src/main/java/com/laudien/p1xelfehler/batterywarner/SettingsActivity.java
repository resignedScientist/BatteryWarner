package com.laudien.p1xelfehler.batterywarner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;
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
        Toolbar toolbar = findViewById(R.id.toolbar);
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
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_faq:
                openLink(getString(R.string.link_faq));
                return true;
            case R.id.menu_xda:
                openLink(getString(R.string.link_xda_thread));
                return true;
            case R.id.menu_donate:
                openLink(getString(R.string.link_donation));
                return true;
            case R.id.menu_contact_developer:
                contactDeveloper();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        onNavigateUp();
    }

    private void contactDeveloper() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.developer_mail)});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            ToastHelper.sendToast(this, "No email app found!");
        }
    }

    private void openLink(String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
