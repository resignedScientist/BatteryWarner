package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;
import com.laudien.p1xelfehler.batterywarner.Activities.BasicGraphFragment;
import com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity.HistoryActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.InfoObject;
import com.laudien.p1xelfehler.batterywarner.Contract;
import com.laudien.p1xelfehler.batterywarner.GraphDbHelper;
import com.laudien.p1xelfehler.batterywarner.R;
import com.laudien.p1xelfehler.batterywarner.Services.ChargingService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.support.annotation.Dimension.SP;
import static com.laudien.p1xelfehler.batterywarner.Contract.IS_PRO;
import static com.laudien.p1xelfehler.batterywarner.GraphDbHelper.TYPE_PERCENTAGE;
import static com.laudien.p1xelfehler.batterywarner.GraphDbHelper.TYPE_TEMPERATURE;

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
    private BroadcastReceiver dischargingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setTimeText();
        }
    };
    private BroadcastReceiver chargingReceiver = new BroadcastReceiver() {
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
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
                Contract.DATABASE_HISTORY_PATH,
                DateFormat.getDateInstance(DateFormat.SHORT)
                        .format(GraphDbHelper.getEndTime(dbHelper.getReadableDatabase()))
                        .replace("/", "_")
        );
        // rename the file if it already exists
        File outputFile = new File(outputFileDir);
        int i = 0;
        String baseFileDir = outputFileDir;
        while (outputFile.exists()) {
            i++;
            outputFileDir = baseFileDir + " (" + i + ")";
            outputFile = new File(outputFileDir);
        }
        File inputFile = context.getDatabasePath(GraphDbHelper.DATABASE_NAME);
        try {
            File directory = new File(Contract.DATABASE_HISTORY_PATH);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            FileInputStream inputStream = new FileInputStream(inputFile);
            FileOutputStream outputStream = new FileOutputStream(outputFile, false);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
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
                setBigText(getString(R.string.disabled_in_settings), true);
            }
        } else {
            setBigText(getString(R.string.not_pro), true);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (IS_PRO && graphEnabled) {
            getContext().registerReceiver(dischargingReceiver, new IntentFilter("android.intent.action.ACTION_POWER_DISCONNECTED"));
            getContext().registerReceiver(chargingReceiver, new IntentFilter("android.intent.action.ACTION_POWER_CONNECTED"));
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
                getContext().unregisterReceiver(dischargingReceiver);
                getContext().unregisterReceiver(chargingReceiver);
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
            Toast.makeText(getContext(), getString(R.string.pro_only_short), Toast.LENGTH_SHORT).show();
            return false;
        }
        switch (id) {
            case R.id.menu_reset:
                if (graphEnabled) {
                    if (series != null) {
                        showResetDialog();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.nothing_to_delete), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), getString(R.string.disabled_in_settings), Toast.LENGTH_SHORT).show();
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
        // first check if all permissions were granted
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        if (requestCode == REQUEST_SAVE_GRAPH) {
            // restart the saving of the graph
            new SaveGraphTask().execute();
        } else if (requestCode == REQUEST_OPEN_HISTORY) {
            openHistory();
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
            setBigText(getString(R.string.not_pro), true);
        }
    }

    /**
     * Sets the text under the GraphView depending on if the device is charging, fully charged or
     * not charging. It also shows if there is no or not enough data to show any graph.
     */
    @Override
    protected void setTimeText() {
        Intent batteryStatus = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isFull = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, Contract.NO_STATE) == 100;
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
                setBigText(getString(R.string.charging_type_disabled), false);
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
            for (Series s : series) {
                graphView.removeSeries(s);
            }
            series = null;
        }
        setTimeText();
    }

    private void showDischargingText() {
        boolean isDatabaseEmpty = series == null;
        if (isDatabaseEmpty) { // no data yet (database is empty)
            setBigText(getString(R.string.no_data), true);
        } else { // database is not empty
            boolean hasEnoughData = infoObject.getTimeInMinutes() != 0;
            if (hasEnoughData) { // enough data
                String timeString = infoObject.getTimeString(getContext());
                setNormalText(String.format(Locale.getDefault(), "%s: %s", getString(R.string.charging_time), timeString));
            } else { // not enough data
                setBigText(getString(R.string.not_enough_data), true);
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
        // check if a graph is present and has enough data
        if (graphView.getSeries().size() == 0 || series[TYPE_PERCENTAGE].getHighestValueX() == 0) {
            Toast.makeText(getContext(), R.string.nothing_to_save, Toast.LENGTH_SHORT).show();
            return;
        }
        // check for permission
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    REQUEST_SAVE_GRAPH
            );
            return;
        }
        // save graph
        new SaveGraphTask().execute();
    }

    /**
     * Shows the dialog to reset the graphs, meaning that the table in the
     * app directory database will be cleared.
     */
    public void showResetDialog() {
        new AlertDialog.Builder(getContext())
                .setCancelable(true)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.question_delete_graph)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        GraphDbHelper graphDbHelper = GraphDbHelper.getInstance(getContext());
                        graphDbHelper.resetTable();
                        Toast.makeText(getContext(), R.string.success_delete_graph, Toast.LENGTH_SHORT).show();
                    }
                }).create().show();
    }

    /**
     * Starts the HistoryActivity after asking for the storage permission.
     */
    public void openHistory() {
        // check for permission
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    REQUEST_OPEN_HISTORY
            );
            return;
        }
        startActivity(new Intent(getContext(), HistoryActivity.class));
    }

    private class SaveGraphTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            return saveGraph(getContext());
        }

        @Override
        protected void onPostExecute(Boolean success) {
            String message;
            if (success) {
                message = getString(R.string.success_saving);
            } else {
                message = getString(R.string.error_saving);
            }
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
