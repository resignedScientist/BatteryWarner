package com.laudien.p1xelfehler.batterywarner.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.HistoryActivity;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseContract;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseModel;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils;
import com.laudien.p1xelfehler.batterywarner.database.DatabaseValue;
import com.laudien.p1xelfehler.batterywarner.database.GraphInfo;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;
import com.laudien.p1xelfehler.batterywarner.services.BackgroundService;

import java.util.Locale;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.support.annotation.Dimension.SP;
import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_BATTERY_LEVEL;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_CURRENT;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_TEMPERATURE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.GRAPH_INDEX_VOLTAGE;
import static com.laudien.p1xelfehler.batterywarner.database.DatabaseUtils.NUMBER_OF_GRAPHS;

/**
 * A Fragment that shows the latest charging curve.
 * It loads the graphs from the database in the app directory and registers a DatabaseChangedListener
 * to refresh automatically with the latest data.
 */
public class GraphFragment extends BasicGraphFragment implements DatabaseContract.DatabaseListener {

    private static final int REQUEST_SAVE_GRAPH = 10;
    private SharedPreferences sharedPreferences;
    private final BroadcastReceiver chargingStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setTimeText();
        }
    };
    private boolean graphEnabled;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (graphEnabled) {
            switches[GRAPH_INDEX_BATTERY_LEVEL].setChecked(
                    sharedPreferences.getBoolean(getString(R.string.pref_checkBox_percent), getResources().getBoolean(R.bool.switch_percentage_default))
            );
            switches[GRAPH_INDEX_TEMPERATURE].setChecked(
                    sharedPreferences.getBoolean(getString(R.string.pref_checkBox_temperature), getResources().getBoolean(R.bool.switch_temperature_default))
            );
            switches[GRAPH_INDEX_CURRENT].setChecked(
                    sharedPreferences.getBoolean(getString(R.string.pref_checkBox_current), getResources().getBoolean(R.bool.switch_current_default))
            );
            switches[GRAPH_INDEX_VOLTAGE].setChecked(
                    sharedPreferences.getBoolean(getString(R.string.pref_checkBox_voltage), getResources().getBoolean(R.bool.switch_voltage_default))
            );
        } else {
            setBigText(getString(R.string.toast_disabled_in_settings), true);
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadSeries();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (graphEnabled) {
            getContext().registerReceiver(chargingStateChangedReceiver, new IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED"));
            getContext().registerReceiver(chargingStateChangedReceiver, new IntentFilter("android.intent.action.ACTION_POWER_CONNECTED"));
            DatabaseModel.getInstance(getContext()).registerDatabaseListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (graphEnabled) {
            sharedPreferences.edit()
                    .putBoolean(getString(R.string.pref_checkBox_percent), switches[GRAPH_INDEX_BATTERY_LEVEL].isChecked())
                    .putBoolean(getString(R.string.pref_checkBox_temperature), switches[GRAPH_INDEX_TEMPERATURE].isChecked())
                    .putBoolean(getString(R.string.pref_checkBox_current), switches[GRAPH_INDEX_CURRENT].isChecked())
                    .putBoolean(getString(R.string.pref_checkBox_voltage), switches[GRAPH_INDEX_VOLTAGE].isChecked())
                    .apply();
            DatabaseModel.getInstance(getContext()).unregisterDatabaseListener(this);
            getContext().unregisterReceiver(chargingStateChangedReceiver);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.reload_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_delete:
                if (graphEnabled) {
                    if (graphs != null) {
                        showResetDialog();
                    } else {
                        ToastHelper.sendToast(getContext(), R.string.toast_nothing_to_delete, LENGTH_SHORT);
                    }
                } else {
                    ToastHelper.sendToast(getContext(), R.string.toast_disabled_in_settings, LENGTH_SHORT);
                }
                return true;
            case R.id.menu_open_history:
                startActivity(new Intent(getContext(), HistoryActivity.class));
                return true;
            case R.id.menu_save_to_history:
                saveGraph();
                return true;
            case R.id.menu_info:
                showInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PERMISSION_GRANTED && requestCode == REQUEST_SAVE_GRAPH) {
            saveGraph(); // restart the saving of the graph
        }
    }

    /**
     * Loads the graphs out of the database from the database file in the app directory.
     *
     * @return Returns an array of the graphs in the database.
     */
    @Override
    protected void readGraphs(boolean useFahrenheit, boolean reverseCurrent, @NonNull DatabaseContract.DataReceiver dataReceiver) {
        DatabaseModel.getInstance(getContext()).readData(useFahrenheit, reverseCurrent, dataReceiver);
    }

    /**
     * Sets the text under the GraphView depending on if the device is charging, fully charged or
     * not charging. It also shows if there is no or not enough data to show a graph.
     */
    @Override
    void setTimeText() {
        Intent batteryStatus = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isFull = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) == 100;
        int chargingType = batteryStatus.getIntExtra(EXTRA_PLUGGED, -1);
        boolean isCharging = chargingType != 0;
        if (isCharging) { // charging
            boolean isChargingTypeEnabled = BackgroundService.isChargingTypeEnabled(getContext(), chargingType, sharedPreferences);
            if (isChargingTypeEnabled) { // charging type enabled
                if (isFull) { // fully charged
                    showDischargingText();
                } else { // not fully charged
                    boolean isDatabaseEmpty = graphs == null || graphInfo == null;
                    String timeString;
                    if (isDatabaseEmpty) {
                        timeString = GraphInfo.getZeroTimeString(getContext());
                    } else {
                        timeString = graphInfo.getTimeString(getContext());
                    }
                    setNormalText(String.format(Locale.getDefault(), "%s... (%s)", getString(R.string.charging), timeString));
                }
            } else { // charging type disabled
                setBigText(getString(R.string.toast_charging_type_disabled), false);
            }
        } else { // discharging
            showDischargingText();
        }
    }

    @Override
    public void onValueAdded(DatabaseValue value, long totalNumberOfRows) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean useFahrenheit = sharedPreferences.getString(getString(R.string.pref_temp_unit), getString(R.string.pref_temp_unit_default)).equals("1");
        boolean reverseCurrent = sharedPreferences.getBoolean(getString(R.string.pref_reverse_current), getResources().getBoolean(R.bool.pref_reverse_current_default));
        DataPoint[] dataPoints = value.toDataPoints(useFahrenheit, reverseCurrent);
        if (dataPoints == null) {
            return;
        }
        if (graphs == null) { // first point
            for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
                if (dataPoints[i] != null) {
                    if (graphs == null) { // only initialize graphs if any DataPoint is not null
                        graphs = new LineGraphSeries[NUMBER_OF_GRAPHS];
                    }
                    graphs[i] = new LineGraphSeries<>(new DataPoint[]{new DataPoint(0d, dataPoints[i].getY())});
                    if (switches[i].isChecked()) {
                        graphView.addSeries(graphs[i]);
                    }
                }
            }
            styleGraphs(graphs);
            graphInfo = new GraphInfo(value, useFahrenheit, reverseCurrent);
        } else { // not the first point
            for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
                if (graphs[i] != null && dataPoints[i] != null) {
                    graphs[i].appendData(dataPoints[i], false, (int) totalNumberOfRows);
                }
            }
            // update graph info
            if (graphInfo != null) {
                graphInfo.notifyValueAdded(value, getContext());
            }
        }
        applyGraphScale();
        enableOrDisableSwitches();
        setTimeText();
    }

    @Override
    public void onTableReset() {
        if (graphInfo != null) {
            graphInfo.dismissDialog();
            graphInfo = null;
        }
        if (graphs != null) {
            graphView.removeAllSeries();
            graphs = null;
        }
        setTimeText();
    }

    private void showDischargingText() {
        boolean isDatabaseEmpty = graphs == null || graphInfo == null;
        if (isDatabaseEmpty) { // no data yet (database is empty or unavailable)
            setBigText(getString(R.string.toast_no_data), true);
        } else { // database is not empty
            LineGraphSeries graph = getFirstAvailableGraph();
            boolean hasEnoughData = graph != null && graph.getHighestValueX() > 0;
            if (hasEnoughData) { // enough data
                String timeString = graphInfo.getTimeString(getContext());
                setNormalText(String.format(Locale.getDefault(), "%s: %s", getString(R.string.info_charging_time), timeString));
            } else { // not enough data
                setBigText(getString(R.string.toast_not_enough_data), true);
            }
        }
    }

    private void setBigText(String disableText, boolean disableCheckBoxes) {
        textView_chargingTime.setText(disableText);
        textView_chargingTime.setTextSize(SP, getResources().getInteger(R.integer.text_size_charging_text_big));
        if (disableCheckBoxes) {
            for (CompoundButton s : switches) {
                s.setEnabled(false);
            }
        }
    }

    private void setNormalText(String enableText) {
        textView_chargingTime.setTextSize(SP, getResources().getInteger(R.integer.text_size_charging_text_normal));
        textView_chargingTime.setText(enableText);
        enableOrDisableSwitches();
    }

    private LineGraphSeries getFirstAvailableGraph() {
        if (graphs != null) {
            for (LineGraphSeries graph : graphs) {
                if (graph != null) {
                    return graph;
                }
            }
        }
        return null;
    }

    private void saveGraph() {
        LineGraphSeries graph = getFirstAvailableGraph();
        // check if there is enough data
        if (graph != null && graphs[GRAPH_INDEX_BATTERY_LEVEL].getHighestValueX() > 0) {
            // check for permission
            if (ContextCompat.checkSelfPermission(getContext(), WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
                // save graph and show toast
                DatabaseUtils.saveGraph(getContext(), new DatabaseUtils.GraphSavedListener() {
                    @Override
                    public void onFinishedSaving(boolean success) {
                        ToastHelper.sendToast(getContext(), success ? R.string.toast_success_saving : R.string.toast_error_saving, LENGTH_SHORT);
                    }
                });
            } else { // permission not granted -> ask for permission
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_SAVE_GRAPH);
            }
        } else { // there is no graph or the graph does not have enough data
            ToastHelper.sendToast(getContext(), R.string.toast_nothing_to_save, LENGTH_SHORT);
        }
    }

    /**
     * Shows the dialog to reset the graphs, meaning that the table in the
     * app directory database will be cleared.
     */
    private void showResetDialog() {
        new AlertDialog.Builder(getContext())
                .setCancelable(true)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.dialog_title_are_you_sure)
                .setMessage(R.string.dialog_message_delete_graph)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .setPositiveButton(R.string.dialog_button_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DatabaseModel.getInstance(getContext()).resetTable();
                        ToastHelper.sendToast(getContext(), R.string.toast_success_delete_graph, LENGTH_SHORT);
                    }
                }).create().show();
    }
}
