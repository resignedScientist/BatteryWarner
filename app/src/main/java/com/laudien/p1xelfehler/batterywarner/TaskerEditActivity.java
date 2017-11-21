package com.laudien.p1xelfehler.batterywarner;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.laudien.p1xelfehler.batterywarner.helper.RootHelper;
import com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper;
import com.laudien.p1xelfehler.batterywarner.helper.TaskerPlugin;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;
import com.twofortyfouram.log.Lumberjack;

import java.util.Calendar;

import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_RESET_GRAPH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SAVE_GRAPH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_SMART_CHARGING_LIMIT;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_SMART_CHARGING_TIME;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_WARNING_HIGH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_SET_WARNING_LOW;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_SMART_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_STOP_CHARGING;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_WARNING_HIGH;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ACTION_TOGGLE_WARNING_LOW;
import static com.laudien.p1xelfehler.batterywarner.helper.TaskerHelper.ALL_ACTIONS;

public class TaskerEditActivity extends AbstractAppCompatPluginActivity {
    private static final int LAYOUT_TIME_PICKER = 0;
    private static final int LAYOUT_SWITCH = 1;
    private static final int LAYOUT_EDIT_TEXT = 2;
    private static final int NUMBER_OF_LAYOUTS = 3;
    private View layouts[] = new View[NUMBER_OF_LAYOUTS];
    private TextView textView_setValue;
    private RadioGroup radioGroup_action;
    private boolean rootAvailable;

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
        layouts[LAYOUT_EDIT_TEXT] = findViewById(R.id.value_edit_text);
        ((TimePicker) layouts[LAYOUT_TIME_PICKER]).setIs24HourView(DateFormat.is24HourFormat(this));
        textView_setValue = findViewById(R.id.textView_set_value);
        enableCorrectLayout(radioGroup_action.getCheckedRadioButtonId());
        // set listeners
        radioGroup_action.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int radioButtonId) {
                enableCorrectLayout(radioButtonId);
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
    protected void onStart() {
        super.onStart();
        checkForRoot();
    }

    @Override
    public boolean isBundleValid(@NonNull Bundle bundle) {
        return TaskerHelper.isVariableBundleValid(this, bundle);
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull Bundle bundle, @NonNull String s) {
        String action = TaskerHelper.getAction(bundle);
        if (action == null) {
            return;
        }
        Object value = bundle.get(action);
        if (value instanceof Boolean) {
            ((Switch) layouts[LAYOUT_SWITCH]).setChecked((Boolean) value);
        } else if (value instanceof Integer || value instanceof String) {
            EditText editText = (EditText) layouts[LAYOUT_EDIT_TEXT];
            editText.setText(String.valueOf(value));
        } else if (value instanceof Long) {
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
        }
        radioGroup_action.check(getRadioButtonId(action));
    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        Bundle resultBundle = null;
        int radioButtonId = radioGroup_action.getCheckedRadioButtonId();
        String action = getAction(radioButtonId);
        switch (action) {
            case ACTION_TOGGLE_CHARGING:
            case ACTION_TOGGLE_STOP_CHARGING:
            case ACTION_TOGGLE_SMART_CHARGING:
            case ACTION_TOGGLE_WARNING_HIGH:
            case ACTION_TOGGLE_WARNING_LOW:
                resultBundle = TaskerHelper.buildBundle(action, ((Switch) layouts[LAYOUT_SWITCH]).isChecked());
                break;
            case ACTION_SET_SMART_CHARGING_LIMIT:
            case ACTION_SET_WARNING_HIGH:
            case ACTION_SET_WARNING_LOW:
                String value = ((EditText) layouts[LAYOUT_EDIT_TEXT]).getText().toString();
                try {
                    int intValue = Integer.valueOf(value);
                    resultBundle = TaskerHelper.buildBundle(action, intValue);
                } catch (NumberFormatException e) {
                    resultBundle = TaskerHelper.buildBundle(action, value);
                }
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
                resultBundle = TaskerHelper.buildBundle(action, calendar.getTimeInMillis());
                break;
            case ACTION_SAVE_GRAPH:
                resultBundle = TaskerHelper.buildBundle(ACTION_SAVE_GRAPH, true);
                break;
            case ACTION_RESET_GRAPH:
                resultBundle = TaskerHelper.buildBundle(ACTION_RESET_GRAPH, true);
                break;
        }
        if (resultBundle != null) {
            if (!TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(resultBundle)) {
                TaskerPlugin.Setting.setVariableReplaceKeys(resultBundle, ALL_ACTIONS);
            }
        }
        return resultBundle;
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull Bundle bundle) {
        String resultBlurb = TaskerHelper.getResultBlurb(this, bundle);
        return resultBlurb != null ? resultBlurb : "Error!";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (checkBundle()) {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (checkBundle()) {
            super.onBackPressed();
        }
    }

    private boolean checkBundle() {
        Bundle resultBundle = getResultBundle();
        if (resultBundle == null || !isBundleValid(resultBundle)) {
            new AlertDialog.Builder(this)
                    .setTitle("Wrong input")
                    .setMessage("The given value is not valid!")
                    .setPositiveButton("Close", null)
                    .create()
                    .show();
            return false;
        }
        if (!rootAvailable && TaskerHelper.checkForRootDependencies(resultBundle)) {
            new AlertDialog.Builder(this)
                    .setTitle("No root available")
                    .setMessage("You have chosen a root dependency but no root is available!")
                    .setPositiveButton("Close", null)
                    .create()
                    .show();
            return false;
        }
        return true;
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
            case R.id.radioButton_set_warning_high:
            case R.id.radioButton_set_warning_low:
                layouts[LAYOUT_EDIT_TEXT].setVisibility(View.VISIBLE);
                break;
            case R.id.radioButton_set_smart_charging_time:
                layouts[LAYOUT_TIME_PICKER].setVisibility(View.VISIBLE);
                break;
            case R.id.radioButton_save_graph:
            case R.id.radioButton_reset_graph:
                textView_setValue.setVisibility(View.GONE);
        }
    }

    private String getAction(int radioButtonId) {
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
            case R.id.radioButton_set_warning_high:
                return ACTION_SET_WARNING_HIGH;
            case R.id.radioButton_set_warning_low:
                return ACTION_SET_WARNING_LOW;
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

    private int getRadioButtonId(String action) {
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
            case ACTION_SET_WARNING_HIGH:
                return R.id.radioButton_set_warning_high;
            case ACTION_SET_WARNING_LOW:
                return R.id.radioButton_set_warning_low;
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

    private void checkForRoot() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                rootAvailable = RootHelper.isRootAvailable();
            }
        });
    }
}
