package com.laudien.p1xelfehler.batterywarner.helper;

import android.view.Window;
import android.view.WindowManager;

public class KeyboardHelper {

    public static void showKeyboard(Window window) {
        if (window != null) { // show keyboard
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }
}
