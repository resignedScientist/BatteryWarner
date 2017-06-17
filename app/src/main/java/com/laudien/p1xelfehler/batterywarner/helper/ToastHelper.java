package com.laudien.p1xelfehler.batterywarner.helper;

import android.content.Context;
import android.widget.Toast;

public class ToastHelper {
    private static Toast toast;

    public static void sendToast(Context context, int stringResource, int duration) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, stringResource, duration);
        toast.show();
    }

    public static void sendToast(Context context, String message) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
