package com.laudien.p1xelfehler.batterywarner;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.laudien.p1xelfehler.batterywarner.helper.NotificationHelper;
import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.log.Lumberjack;

import java.util.Calendar;

import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_RESET_GRAPH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SAVE_GRAPH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_SMART_CHARGING_LIMIT;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_SMART_CHARGING_TIME;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_SMART_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_STOP_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_WARNING_HIGH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_WARNING_LOW;

public class TaskerEditActivity extends AbstractAppCompatPluginActivity {
    private static final int LAYOUT_TIME_PICKER = 0;
    private static final int LAYOUT_SWITCH = 1;
    private static final int LAYOUT_NUMBER_PICKER = 2;
    private static final int NUMBER_OF_LAYOUTS = 3;
    private View layouts[] = new View[NUMBER_OF_LAYOUTS];
    private TextView textView_setValue;
    private RadioGroup radioGroup_action;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // apply the theme
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(getString(R.string.pref_dark_theme_enabled), getResources().getBoolean(R.bool.pref_dark_theme_enabled_default))) {
            setTheme(R.style.AppTheme_Dark);
        }
        // init
        setContentView(R.layout.activity_tasker_edit);
        radioGroup_action = findViewById(R.id.radio_group_action);
        layouts[LAYOUT_TIME_PICKER] = findViewById(R.id.value_time_picker);
        layouts[LAYOUT_SWITCH] = findViewById(R.id.value_switch);
        layouts[LAYOUT_NUMBER_PICKER] = findViewById(R.id.value_number_picker);
        textView_setValue = findViewById(R.id.textView_set_value);
        enableCorrectLayout(radioGroup_action.getCheckedRadioButtonId());
        // set listeners
        radioGroup_action.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int radioButtonId) {
                enableCorrectLayout(radioButtonId);
            }
        });
        // make a root check
        final Context context = getApplicationContext();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (!RootHelper.isRootAvailable()) {
                    NotificationHelper.showNotification(context, NotificationHelper.ID_NOT_ROOTED);
                }
            }
        });
        // configure the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        try {
            CharSequence title =
                    getPackageManager().getApplicationLabel(
                            getPackageManager().getApplicationInfo(getCallingPackage(),
                                    0));
            toolbar.setTitle(title);
        } catch (final PackageManager.NameNotFoundException e) {
            Lumberjack.e("Calling package couldn't be found%s", e); //$NON-NLS-1$
        }
        toolbar.setSubtitle(getString(R.string.tasker_plugin_name));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean isBundleValid(@NonNull Bundle bundle) {
        return TaskerHelper.isBundleValid(bundle);
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull Bundle bundle, @NonNull String s) {
        int action = TaskerHelper.getAction(bundle);
        Object value = TaskerHelper.getValue(bundle);
        switch (action) {
            case ACTION_TOGGLE_CHARGING:
            case ACTION_TOGGLE_STOP_CHARGING:
            case ACTION_TOGGLE_SMART_CHARGING:
            case ACTION_TOGGLE_WARNING_HIGH:
            case ACTION_TOGGLE_WARNING_LOW:
                ((Switch) layouts[LAYOUT_SWITCH]).setChecked((Boolean) value);
                break;
            case ACTION_SET_SMART_CHARGING_LIMIT:
                NumberPicker numberPicker = (NumberPicker) layouts[LAYOUT_NUMBER_PICKER];
                numberPicker.setValue((Integer) value);
                break;
            case ACTION_SET_SMART_CHARGING_TIME:
                TimePicker timePicker = (TimePicker) layouts[LAYOUT_TIME_PICKER];
                long timeInMillis = (long) value;
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(timeInMillis);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    timePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
                    timePicker.setMinute(calendar.get(Calendar.MINUTE));
                } else {
                    timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
                    timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
                }
                break;
        }
        radioGroup_action.check(getRadioButtonId(action));
    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        int radioButtonId = radioGroup_action.getCheckedRadioButtonId();
        int action = getAction(radioButtonId);
        Object value = null;
        switch (action) {
            case ACTION_TOGGLE_CHARGING:
            case ACTION_TOGGLE_STOP_CHARGING:
            case ACTION_TOGGLE_SMART_CHARGING:
            case ACTION_TOGGLE_WARNING_HIGH:
            case ACTION_TOGGLE_WARNING_LOW:
                value = ((Switch) layouts[LAYOUT_SWITCH]).isChecked();
                break;
            case ACTION_SET_SMART_CHARGING_LIMIT:
                value = ((NumberPicker) layouts[LAYOUT_NUMBER_PICKER]).getValue();
                break;
            case ACTION_SET_SMART_CHARGING_TIME:
                TimePicker timePicker = (TimePicker) layouts[LAYOUT_TIME_PICKER];
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.MILLISECOND, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                    calendar.set(Calendar.MINUTE, timePicker.getMinute());
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
                    calendar.set(Calendar.MINUTE, timePicker.getCurrentMinute());
                }
                value = calendar.getTimeInMillis();
                break;
        }
        return TaskerHelper.buildBundle(action, value);
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull Bundle bundle) {
        return TaskerHelper.getResultBlurb(this, bundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableCorrectLayout(int radioButtonId) {
        // first disable all layouts
        for (View layout : layouts) {
            layout.setVisibility(View.GONE);
        }
        textView_setValue.setVisibility(View.VISIBLE);
        // then enable the correct layout
        switch (radioButtonId) {
            case R.id.radioButton_toggle_charging:
            case R.id.radioButton_toggle_stop_charging:
            case R.id.radioButton_toggle_smart_charging:
            case R.id.radioButton_toggle_warning_high:
            case R.id.radioButton_toggle_warning_low:
                layouts[LAYOUT_SWITCH].setVisibility(View.VISIBLE);
                break;
            case R.id.radioButton_set_smart_charging_limit:
                NumberPicker numberPicker = (NumberPicker) layouts[LAYOUT_NUMBER_PICKER];
                numberPicker.setVisibility(View.VISIBLE);
                numberPicker.setMinValue(getResources().getInteger(R.integer.pref_smart_charging_limit_min));
                numberPicker.setMaxValue(getResources().getInteger(R.integer.pref_smart_charging_limit_max));
                break;
            case R.id.radioButton_set_smart_charging_time:
                layouts[LAYOUT_TIME_PICKER].setVisibility(View.VISIBLE);
                break;
            case R.id.radioButton_save_graph:
            case R.id.radioButton_reset_graph:
                textView_setValue.setVisibility(View.GONE);
        }
    }

    private int getAction(int radioButtonId) {
        switch (radioButtonId) {
            case R.id.radioButton_toggle_charging:
                return ACTION_TOGGLE_CHARGING;
            case R.id.radioButton_toggle_stop_charging:
                return ACTION_TOGGLE_STOP_CHARGING;
            case R.id.radioButton_toggle_smart_charging:
                return ACTION_TOGGLE_SMART_CHARGING;
            case R.id.radioButton_toggle_warning_high:
                return ACTION_TOGGLE_WARNING_HIGH;
            case R.id.radioButton_toggle_warning_low:
                return ACTION_TOGGLE_WARNING_LOW;
            case R.id.radioButton_set_smart_charging_limit:
                return ACTION_SET_SMART_CHARGING_LIMIT;
            case R.id.radioButton_set_smart_charging_time:
                return ACTION_SET_SMART_CHARGING_TIME;
            case R.id.radioButton_save_graph:
                return ACTION_SAVE_GRAPH;
            case R.id.radioButton_reset_graph:
                return ACTION_RESET_GRAPH;
            default:
                return ACTION_TOGGLE_CHARGING;
        }
    }

    private int getRadioButtonId(int action) {
        switch (action) {
            case ACTION_TOGGLE_CHARGING:
                return R.id.radioButton_toggle_charging;
            case ACTION_TOGGLE_STOP_CHARGING:
                return R.id.radioButton_toggle_stop_charging;
            case ACTION_TOGGLE_SMART_CHARGING:
                return R.id.radioButton_toggle_smart_charging;
            case ACTION_TOGGLE_WARNING_HIGH:
                return R.id.radioButton_toggle_warning_high;
            case ACTION_TOGGLE_WARNING_LOW:
                return R.id.radioButton_toggle_warning_low;
            case ACTION_SET_SMART_CHARGING_LIMIT:
                return R.id.radioButton_set_smart_charging_limit;
            case ACTION_SET_SMART_CHARGING_TIME:
                return R.id.radioButton_set_smart_charging_time;
            case ACTION_SAVE_GRAPH:
                return R.id.radioButton_save_graph;
            case ACTION_RESET_GRAPH:
                return R.id.radioButton_reset_graph;
            default:
                throw new RuntimeException("Unknown action!");
        }
    }
}
