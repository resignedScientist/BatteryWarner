package com.laudien.p1xelfehler.batterywarner.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

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
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("*/*");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, "kontakt@norman-laudien.de");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Indian translation");
        if (emailIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(emailIntent);
        }
    }
}
