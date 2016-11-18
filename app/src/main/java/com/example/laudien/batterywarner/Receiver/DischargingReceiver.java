package com.example.laudien.batterywarner.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.laudien.batterywarner.Receiver.BatteryAlarmReceiver;

public class DischargingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) return;

        BatteryAlarmReceiver.cancelExistingAlarm(context);
        // TODO: only set new alarm if enabled in settings!
        BatteryAlarmReceiver.setRepeatingAlarm(context, false);
    }
}
