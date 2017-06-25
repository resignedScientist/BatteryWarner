package com.laudien.p1xelfehler.batterywarner.services;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;

/**
 * Service to handle button clicks on event notifications.
 * This might do different things at different app versions.
 */
public class EventService extends IntentService {

    public EventService() {
        super(null);
    }

    public EventService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setType("text/plain");
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"kontakt@norman-laudien.de"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Indian translation");
        if (emailIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
            getApplicationContext().startActivity(emailIntent);
        } else {
            ToastHelper.sendToast(getApplicationContext(), "No email app found!");
        }
    }
}
