package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
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

import static android.support.annotation.Dimension.SP;
import static com.laudien.p1xelfehler.batterywarner.Contract.IS_PRO;
import static com.laudien.p1xelfehler.batterywarner.GraphDbHelper.TYPE_PERCENTAGE;
import static com.laudien.p1xelfehler.batterywarner.GraphDbHelper.TYPE_TEMPERATURE;

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
            if (!ChargingService.isChargingTypeEnabled(context)) {
                disable(getString(R.string.charging_type_disabled), false);
            }
        }
    };

    public static void saveGraph(Context context) {
        // return if permissions are not granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(context.getString(R.string.pref_graph_autosave), false)
                    .apply();
            return;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean graphEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_graph_enabled), context.getResources().getBoolean(R.bool.pref_graph_enabled_default));
        // return if not pro or graph disabled in settings or the database has not enough data
        if (!IS_PRO || !graphEnabled || !GraphDbHelper.getInstance(context).hasEnoughData()) {
            return;
        }

        GraphDbHelper dbHelper = GraphDbHelper.getInstance(context);
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
            Toast.makeText(context, R.string.success_saving, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, R.string.error_saving, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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
                checkBox_percentage.setChecked(
                        sharedPreferences.getBoolean(getString(R.string.pref_checkBox_percent), getResources().getBoolean(R.bool.pref_checkBox_percent_default))
                );
                checkBox_temp.setChecked(
                        sharedPreferences.getBoolean(getString(R.string.pref_checkBox_temperature), getResources().getBoolean(R.bool.pref_checkBox_temperature_default))
                );
                graphDbHelper = GraphDbHelper.getInstance(getContext());
            } else {
                disable(getString(R.string.disabled_in_settings), true);
            }
        } else {
            disable(getString(R.string.not_pro), true);
        }
        return view;
    }

    private void disable(String disableText, boolean disableCheckBoxes) {
        textView_chargingTime.setText(disableText);
        textView_chargingTime.setTextSize(SP, 18);
        if (disableCheckBoxes) {
            checkBox_temp.setEnabled(false);
            checkBox_percentage.setEnabled(false);
        }
    }

    @Override
    protected LineGraphSeries<DataPoint>[] getSeries() {
        if (IS_PRO) {
            GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
            return dbHelper.getGraphs(getContext());
        }
        return null;
    }

    @Override
    protected long getEndDate() {
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        return GraphDbHelper.getEndTime(dbHelper.getReadableDatabase());
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
    protected void loadSeries() {
        if (IS_PRO) {
            super.loadSeries();
        } else {
            textView_chargingTime.setText(getString(R.string.not_pro));
            textView_chargingTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            checkBox_temp.setEnabled(false);
            checkBox_percentage.setEnabled(false);
        }
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
            if (!ChargingService.isChargingTypeEnabled(getContext())) {
                disable(getString(R.string.charging_type_disabled), false);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (graphEnabled) {
            sharedPreferences.edit()
                    .putBoolean(getString(R.string.pref_checkBox_percent), checkBox_percentage.isChecked())
                    .putBoolean(getString(R.string.pref_checkBox_temperature), checkBox_temp.isChecked())
                    .apply();
            if (IS_PRO) {
                graphDbHelper.setDatabaseChangedListener(null);
                getContext().unregisterReceiver(dischargingReceiver);
                getContext().unregisterReceiver(chargingReceiver);
            }
        }
    }

    @Override
    protected void setTimeText() {
        Intent batteryStatus = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) {
            return;
        }
        boolean isFull = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, Contract.NO_STATE) == BatteryManager.BATTERY_STATUS_FULL;
        boolean isCharging = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, Contract.NO_STATE) != 0;
        if (series != null) {
            String timeString = infoObject.getTimeString(getContext());
            if (infoObject.getTimeInMinutes() != 0) { // enough data
                if (isCharging && !isFull) {
                    textView_chargingTime.setText(String.format("%s (%s)", getString(R.string.charging), timeString));
                } else {
                    textView_chargingTime.setText(String.format("%s: %s", getString(R.string.charging_time), timeString));
                }
            } else { // not enough data
                if (isCharging && !isFull) {
                    textView_chargingTime.setText(String.format("%s (%s)", getString(R.string.charging), InfoObject.getZeroTimeString(getContext())));
                } else {
                    textView_chargingTime.setText(getString(R.string.not_enough_data));
                }
            }
        } else { // empty database
            if (isCharging && !isFull) {
                textView_chargingTime.setText(String.format("%s (%s)", getString(R.string.charging), InfoObject.getZeroTimeString(getContext())));
            } else {
                textView_chargingTime.setText(getString(R.string.no_data));
            }
        }
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
            saveGraph();
        } else if (requestCode == REQUEST_OPEN_HISTORY) {
            openHistory();
        }
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
        saveGraph(getContext());
    }

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
                        ChargingService.restartService(getContext());
                        Toast.makeText(getContext(), R.string.success_delete_graph, Toast.LENGTH_SHORT).show();
                    }
                }).create().show();
    }

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

    @Override
    public void onValueAdded(double timeInMinutes, int percentage, int temperature) {
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
}
