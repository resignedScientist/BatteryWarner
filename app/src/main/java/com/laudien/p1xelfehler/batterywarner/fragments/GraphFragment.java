package com.laudien.p1xelfehler.batterywarner.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.HistoryActivity;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.helper.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.helper.ToastHelper;
import com.laudien.p1xelfehler.batterywarner.services.ChargingService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.support.annotation.Dimension.SP;
import static android.widget.Toast.LENGTH_SHORT;
import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.DATABASE_HISTORY_PATH;
import static com.laudien.p1xelfehler.batterywarner.AppInfoHelper.IS_PRO;
import static com.laudien.p1xelfehler.batterywarner.helper.GraphDbHelper.DATABASE_NAME;
import static com.laudien.p1xelfehler.batterywarner.helper.GraphDbHelper.TYPE_PERCENTAGE;
import static com.laudien.p1xelfehler.batterywarner.helper.GraphDbHelper.TYPE_TEMPERATURE;
import static java.text.DateFormat.SHORT;

/**
 * A Fragment that shows the latest charging curve.
 * It loads the graphs from the database in the app directory and registers a DatabaseChangedListener
 * to refresh automatically with the latest data.
 */
public class GraphFragment extends BasicGraphFragment implements GraphDbHelper.DatabaseChangedListener {

    private static final int REQUEST_SAVE_GRAPH = 10;
    private static final int REQUEST_OPEN_HISTORY = 20;
    private SharedPreferences sharedPreferences;
    private GraphDbHelper graphDbHelper;
    private boolean graphEnabled;
    private BroadcastReceiver chargingStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setTimeText();
        }
    };

    /**
     * Saves the graph in the app directory to the database directory in the external storage.
     * Can only run outside of the main/ui thread!
     *
     * @param context An instance of the Context class.
     * @return Returns true if the saving process was successful, false if not.
     */
    public static boolean saveGraph(Context context) {
        Log.d("GraphSaver", "Saving graph...");
        // throw exception if in main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("Do not save the graph in main thread!");
        }
        // return if permissions are not granted
        if (ContextCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(context.getString(R.string.pref_graph_autosave), false)
                    .apply();
            return false;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean graphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(context);
        // return if not pro or graph disabled in settings or the database has not enough data
        if (!IS_PRO || !graphEnabled || !dbHelper.hasEnoughData()) {
            return false;
        }
        String outputFileDir = String.format(
                Locale.getDefault(),
                "%s/%s",
                DATABASE_HISTORY_PATH,
                DateFormat.getDateInstance(SHORT)
                        .format(GraphDbHelper.getEndTime(dbHelper.getReadableDatabase()))
                        .replace("/", "_")
        );
        // rename the file if it already exists
        File outputFile = new File(outputFileDir);
        String baseFileDir = outputFileDir;
        for (byte i = 1; outputFile.exists() && i < 127; i++){
            outputFileDir = baseFileDir + " (" + i + ")";
            outputFile = new File(outputFileDir);
        }
        File inputFile = context.getDatabasePath(DATABASE_NAME);
        try {
            File directory = new File(DATABASE_HISTORY_PATH);
            if (!directory.exists()) {
                if (!directory.mkdirs()){
                    return false;
                }
            }
            FileInputStream inputStream = new FileInputStream(inputFile);
            FileOutputStream outputStream = new FileOutputStream(outputFile, false);
            byte[] buffer = new byte[1024];
            while (inputStream.read(buffer) != -1) {
                outputStream.write(buffer);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Log.d("GraphSaver", "Graph saved!");
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default));
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (IS_PRO) {
            if (graphEnabled) {
                switch_percentage.setChecked(
                        sharedPreferences.getBoolean(getString(R.string.pref_checkBox_percent), getResources().getBoolean(R.bool.pref_checkBox_percent_default))
                );
                switch_temp.setChecked(
                        sharedPreferences.getBoolean(getString(R.string.pref_checkBox_temperature), getResources().getBoolean(R.bool.pref_checkBox_temperature_default))
                );
                graphDbHelper = GraphDbHelper.getInstance(getContext());
            } else {
                setBigText(getString(R.string.toast_disabled_in_settings), true);
            }
        } else {
            setBigText(getString(R.string.toast_not_pro), true);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (IS_PRO && graphEnabled) {
            getContext().registerReceiver(chargingStateChangedReceiver, new IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED"));
            getContext().registerReceiver(chargingStateChangedReceiver, new IntentFilter("android.intent.action.ACTION_POWER_CONNECTED"));
            graphDbHelper.setDatabaseChangedListener(this);
            if (graphDbHelper.hasDbChanged()) {
                reload();
            } else {
                setTimeText();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (graphEnabled) {
            sharedPreferences.edit()
                    .putBoolean(getString(R.string.pref_checkBox_percent), switch_percentage.isChecked())
                    .putBoolean(getString(R.string.pref_checkBox_temperature), switch_temp.isChecked())
                    .apply();
            if (IS_PRO) {
                graphDbHelper.setDatabaseChangedListener(null);
                getContext().unregisterReceiver(chargingStateChangedReceiver);
            }
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
        if (!IS_PRO && id != R.id.menu_open_history && id != R.id.menu_settings) {
            ToastHelper.sendToast(getContext(), R.string.toast_not_pro_short, LENGTH_SHORT);
            return false;
        }
        switch (id) {
            case R.id.menu_reset:
                if (graphEnabled) {
                    if (series != null) {
                        showResetDialog();
                    } else {
                        ToastHelper.sendToast(getContext(), R.string.toast_nothing_to_delete, LENGTH_SHORT);
                    }
                } else {
                    ToastHelper.sendToast(getContext(), R.string.toast_disabled_in_settings, LENGTH_SHORT);
                }
                break;
            case R.id.menu_open_history:
                openHistory();
                return true;
            case R.id.menu_save_to_history:
                saveGraph();
                return true;
            case R.id.menu_info:
                showInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PERMISSION_GRANTED) {
            if (requestCode == REQUEST_SAVE_GRAPH) {
                new SaveGraphTask().execute(); // restart the saving of the graph
            } else if (requestCode == REQUEST_OPEN_HISTORY) {
                openHistory();
            }
        }
    }

    /**
     * Loads the graphs out of the database from the database file in the app directory.
     *
     * @return Returns an array of the graphs in the database or null if it is not the pro version
     * of the app.
     */
    @Override
    protected LineGraphSeries<DataPoint>[] getSeries() {
        if (IS_PRO) {
            GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
            return dbHelper.getGraphs(getContext());
        }
        return null;
    }

    /**
     * Returns the date the graph was created in milliseconds.
     *
     * @return Returns the date the graph was created in milliseconds.
     */
    @Override
    protected long getCreationTime() {
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        return GraphDbHelper.getEndTime(dbHelper.getReadableDatabase());
    }

    /**
     * Loads the graphs only if it is the pro version of the app. Otherwise it shows the
     * not-pro text under the GraphView.
     */
    @Override
    protected void loadSeries() {
        if (IS_PRO) {
            super.loadSeries();
        } else {
            setBigText(getString(R.string.toast_not_pro), true);
        }
    }

    /**
     * Sets the text under the GraphView depending on if the device is charging, fully charged or
     * not charging. It also shows if there is no or not enough data to show a graph.
     */
    @Override
    protected void setTimeText() {
        Intent batteryStatus = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isFull = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) == 100;
        int chargingType = batteryStatus.getIntExtra(EXTRA_PLUGGED, -1);
        boolean isCharging = chargingType != 0;
        if (isCharging) { // charging
            boolean isChargingTypeEnabled = ChargingService.isChargingTypeEnabled(getContext(), chargingType, sharedPreferences);
            if (isChargingTypeEnabled) { // charging type enabled
                if (isFull) { // fully charged
                    showDischargingText();
                } else { // not fully charged
                    boolean isDatabaseEmpty = series == null;
                    String timeString;
                    if (isDatabaseEmpty) {
                        timeString = InfoObject.getZeroTimeString(getContext());
                    } else {
                        timeString = infoObject.getTimeString(getContext());
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

    /**
     * Comes from the DatabaseChangedListener. It adds the new value to the graphs in the GraphView.
     *
     * @param timeInMinutes The time difference between the time of the first point and this point in minutes.
     * @param percentage    The battery level that was added.
     * @param temperature   The battery temperature that was added.
     */
    @Override
    public void onValueAdded(double timeInMinutes, int percentage, double temperature) {
        if (series != null) {
            series[TYPE_PERCENTAGE].appendData(new DataPoint(timeInMinutes, percentage), true, 1000);
            series[TYPE_TEMPERATURE].appendData(new DataPoint(timeInMinutes, temperature), true, 1000);
            Viewport viewport = graphView.getViewport();
            viewport.setMinX(0);
            viewport.setMaxX(series[TYPE_PERCENTAGE].getHighestValueX());
            infoObject.updateValues(
                    Calendar.getInstance().getTimeInMillis(),
                    timeInMinutes,
                    series[TYPE_TEMPERATURE].getHighestValueY(),
                    series[TYPE_TEMPERATURE].getLowestValueY(),
                    series[TYPE_PERCENTAGE].getHighestValueY() - series[TYPE_PERCENTAGE].getLowestValueY()
            );
            setTimeText();
        } else {
            loadSeries();
        }
    }

    /**
     * Comes from the DatabaseChangedListener. It removes all graphs from the GraphView if there are
     * any graphs and sets the text under the GraphView with the setTimeText() method.
     */
    @Override
    public void onDatabaseCleared() {
        if (series != null) {
            graphView.removeAllSeries();
            series = null;
        }
        setTimeText();
    }

    private void showDischargingText() {
        boolean isDatabaseEmpty = series == null;
        if (isDatabaseEmpty) { // no data yet (database is empty)
            setBigText(getString(R.string.toast_no_data), true);
        } else { // database is not empty
            boolean hasEnoughData = infoObject.getTimeInMinutes() != 0;
            if (hasEnoughData) { // enough data
                String timeString = infoObject.getTimeString(getContext());
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
            switch_temp.setEnabled(false);
            switch_percentage.setEnabled(false);
        }
    }

    private void setNormalText(String enableText) {
        textView_chargingTime.setTextSize(SP, getResources().getInteger(R.integer.text_size_charging_text_normal));
        textView_chargingTime.setText(enableText);
        switch_temp.setEnabled(true);
        switch_percentage.setEnabled(true);
    }

    private void saveGraph() {
        if (graphView.getSeries().size() > 0 && series[TYPE_PERCENTAGE].getHighestValueX() > 0) {
            // check for permission
            if (ContextCompat.checkSelfPermission(getContext(), WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
                // save graph
                new SaveGraphTask().execute();
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
    public void showResetDialog() {
        new AlertDialog.Builder(getContext())
                .setCancelable(true)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.dialog_title_are_you_sure)
                .setMessage(R.string.dialog_message_delete_graph)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .setPositiveButton(R.string.dialog_button_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        GraphDbHelper graphDbHelper = GraphDbHelper.getInstance(getContext());
                        graphDbHelper.resetTable();
                        ToastHelper.sendToast(getContext(), R.string.toast_success_delete_graph, LENGTH_SHORT);
                    }
                }).create().show();
    }

    /**
     * Starts the HistoryActivity after asking for the storage permission.
     */
    public void openHistory() {
        if (ContextCompat.checkSelfPermission(getContext(), READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            startActivity(new Intent(getContext(), HistoryActivity.class)); // open history
        } else { // permission not granted -> ask for permission
            requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, REQUEST_OPEN_HISTORY);
        }
    }

    private class SaveGraphTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            return saveGraph(getContext());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            int message;
            if (success) {
                message = R.string.toast_success_saving;
            } else {
                message = R.string.toast_error_saving;
            }
            ToastHelper.sendToast(getContext(), message, LENGTH_SHORT);
        }
    }
}
