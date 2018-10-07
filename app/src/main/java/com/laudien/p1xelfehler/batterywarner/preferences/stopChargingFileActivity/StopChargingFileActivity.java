package com.laudien.p1xelfehler.batterywarner.preferences.stopChargingFileActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.laudien.p1xelfehler.batterywarner.BaseActivity;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

import static android.os.Build.VERSION.SDK_INT;

public class StopChargingFileActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_base_layout);
        setToolbarTitle(getString(R.string.title_stop_charging_file));
        // replace container layout with StopChargingFileFragment
        getFragmentManager().beginTransaction().replace(R.id.container_layout, new StopChargingFileFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.stop_charging_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_sent_file_to_developer) {
            sendFileToDeveloper();
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendFileToDeveloper() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.developer_mail)});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT,
                "\n------------------" +
                        "\nDebug information" +
                        "\n------------------" +
                        "\nApp version: " + getAppVersion() +
                        "\nAndroid version: " + SDK_INT +
                        "\nDevice: " + Build.DEVICE +
                        "\nBrand: " + Build.BRAND +
                        "\nModel: " + Build.MODEL +
                        "\nProduct: " + Build.PRODUCT +
                        "\nFile path: " + prefs.getString(getString(R.string.pref_stop_charging_file), "") +
                        "\nchargeOn: " + prefs.getString(getString(R.string.pref_stop_charging_enable_charging_text), getString(R.string.pref_stop_charging_enable_charging_text_default)) +
                        "\nchargeOff: " + prefs.getString(getString(R.string.pref_stop_charging_disable_charging_text), getString(R.string.pref_stop_charging_disable_charging_text_default))
        );
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            ToastHelper.sendToast(this, R.string.toast_no_email_app);
        }
    }

    @SuppressLint("DefaultLocale")
    @Nullable
    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return String.format("%s (%d)", pInfo.versionName, pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
