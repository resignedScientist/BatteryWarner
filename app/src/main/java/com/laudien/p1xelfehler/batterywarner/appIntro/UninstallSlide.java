package com.laudien.p1xelfehler.batterywarner.appIntro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.laudien.p1xelfehler.batterywarner.AppInfoHelper;
import com.laudien.p1xelfehler.batterywarner.R;

import agency.tango.materialintroscreen.SlideFragment;

public class UninstallSlide extends SlideFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.slide_uninstall, container, false);
        Button uninstallButton = (Button) view.findViewById(R.id.btn_uninstall);
        uninstallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final int packageId = getContext().getPackageManager().getPackageInfo(AppInfoHelper.PACKAGE_NAME_FREE, 0).applicationInfo.uid;
                    Uri uri = Uri.parse("package:" + AppInfoHelper.PACKAGE_NAME_FREE);
                    Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
                    IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
                    intentFilter.addDataScheme("package");
                    getContext().registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (intent != null && intent.hasExtra(Intent.EXTRA_UID) && intent.getIntExtra(Intent.EXTRA_UID, 0) == packageId) {
                                next();
                            }
                        }
                    }, intentFilter);
                    startActivity(uninstallIntent);
                } catch (PackageManager.NameNotFoundException e) {
                    next();
                }
            }
        });
        return view;
    }

    @Override
    public int backgroundColor() {
        return R.color.colorIntro3;
    }

    @Override
    public int buttonsColor() {
        return R.color.colorButtons;
    }

    @Override
    public boolean canMoveFurther() {
        try {
            getContext().getPackageManager()
                    .getPackageInfo(AppInfoHelper.PACKAGE_NAME_FREE, PackageManager.GET_ACTIVITIES);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }
}
