package com.laudien.p1xelfehler.batterywarner.fragments;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;
import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;
import com.laudien.p1xelfehler.batterywarner.views.BatteryView;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.BatteryData.INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.BatteryData.INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.BatteryData.INDEX_HEALTH;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.BatteryData.INDEX_TECHNOLOGY;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.BatteryData.INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.services.BackgroundService.BatteryData.INDEX_VOLTAGE;

public class MainPageFragment extends Fragment implements BackgroundService.BatteryValueChangedListener {

    private static final byte COLOR_LOW = 1;
    private static final byte COLOR_HIGH = 2;
    private static final byte COLOR_OK = 3;
    @Nullable
    private
    BackgroundService.BackgroundServiceBinder serviceBinder;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            serviceBinder = (BackgroundService.BackgroundServiceBinder) iBinder;
            serviceBinder.setBatteryValueChangedListener(MainPageFragment.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
    private byte currentColor = 0;
    private int warningLow, warningHigh;
    private SharedPreferences sharedPreferences;
    private TextView textView_current, textView_technology,
            textView_temp, textView_health, textView_batteryLevel, textView_voltage;
    private BatteryView img_battery;
    private int clicks = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);
        img_battery = view.findViewById(R.id.img_battery);
        textView_technology = view.findViewById(R.id.textView_technology);
        textView_temp = view.findViewById(R.id.textView_temp);
        textView_health = view.findViewById(R.id.textView_health);
        textView_batteryLevel = view.findViewById(R.id.textView_batteryLevel);
        textView_voltage = view.findViewById(R.id.textView_voltage);
        textView_current = view.findViewById(R.id.textView_current);
        img_battery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clicks++;
                if (clicks > 4) {
                    showTheDestroyer();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clicks = 0;
                    }
                }, 3000);
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        warningLow = sharedPreferences.getInt(getString(R.string.pref_warning_low), getResources().getInteger(R.integer.pref_warning_low_default));
        warningHigh = sharedPreferences.getInt(getString(R.string.pref_warning_high), getResources().getInteger(R.integer.pref_warning_high_default));
        Context context = getContext();
        if (context != null) {
            // bind background service
            context.bindService(new Intent(context, BackgroundService.class), serviceConnection, 0);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (serviceBinder != null) {
            serviceBinder.removeBatteryValueChangedListener();
        }
        Context context = getContext();
        if (context != null) {
            getContext().unbindService(serviceConnection);
        }
    }

    @Override
    public void onBatteryValueChanged(BackgroundService.BatteryData batteryData, int index) {
        String infoString = batteryData.getLongValueString(index);
        switch (index) {
            case INDEX_TECHNOLOGY:
                textView_technology.setText(infoString);
                break;
            case INDEX_TEMPERATURE:
                textView_temp.setText(infoString);
                break;
            case INDEX_HEALTH:
                textView_health.setText(infoString);
                break;
            case INDEX_BATTERY_LEVEL:
                textView_batteryLevel.setText(infoString);
                setBatteryColor(batteryData.getBatteryLevel());
                break;
            case INDEX_VOLTAGE:
                textView_voltage.setText(infoString);
                break;
            case INDEX_CURRENT:
                if (SDK_INT >= LOLLIPOP) {
                    textView_current.setText(infoString);
                }
                break;
        }
    }

    private void setBatteryColor(int batteryLevel) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        byte nextColor;
        if (batteryLevel <= warningLow) { // battery low
            nextColor = COLOR_LOW;
        } else if (batteryLevel < warningHigh) { // battery ok
            nextColor = COLOR_OK;
        } else { // battery high
            nextColor = COLOR_HIGH;
        }
        if (nextColor != currentColor) {
            currentColor = nextColor;
            switch (nextColor) {
                case COLOR_LOW:
                    img_battery.setColor(context.getResources().getColor(R.color.colorBatteryLow));
                    break;
                case COLOR_OK:
                    img_battery.setColor(context.getResources().getColor(R.color.colorBatteryOk));
                    break;
                case COLOR_HIGH:
                    img_battery.setColor(context.getResources().getColor(R.color.colorBatteryHigh));
                    break;
            }
        }
    }

    private void showTheDestroyer() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        ToastHelper.sendToast(context, "DESTROY!");
        final Dialog dialog = new Dialog(context);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_destroyer);
        // close button
        Button btn_close = dialog.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                ToastHelper.sendToast(getContext(), "TRAIN HARD!");
                Uri geoLocation = Uri.parse("geo:0,0?q=gym");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);
                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
        // credits
        TextView credits = dialog.findViewById(R.id.textView_credits);
        credits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?user_id=fatalmerlin"));
                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else { // no twitter app, open browser
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/fatalmerlin"));
                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            }
        });
        dialog.show();
    }
}
