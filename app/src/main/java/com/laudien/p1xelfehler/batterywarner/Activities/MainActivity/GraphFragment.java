package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.Manifest;
import android.app.Activity;
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
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import java.util.Locale;

import static com.laudien.p1xelfehler.batterywarner.GraphDbHelper.TYPE_PERCENTAGE;

public class GraphFragment extends BasicGraphFragment {

    private static final String TAG = "GraphFragment";
    private static final int REQUEST_SAVE_GRAPH = 10;
    private static final int REQUEST_OPEN_HISTORY = 20;
    private boolean graphEnabled;
    private SharedPreferences sharedPreferences;
    private BroadcastReceiver dbChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reload();
        }
    };

    public static void notify(Context context) {
        Intent intent = new Intent();
        intent.setAction(Contract.BROADCAST_STATUS_CHANGED);
        context.sendBroadcast(intent);
    }

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
        if (!Contract.IS_PRO || !graphEnabled || !GraphDbHelper.getInstance(context).hasEnoughData()) {
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
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected Series[] getSeries() {
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        return dbHelper.getGraphs(getContext());
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
        if (!Contract.IS_PRO && id != R.id.menu_open_history && id != R.id.menu_settings) {
            Toast.makeText(getContext(), getString(R.string.pro_only_short), Toast.LENGTH_SHORT).show();
            return false;
        }
        switch (id) {
            case R.id.menu_refresh:
                if (graphEnabled) {
                    reload();
                    Toast.makeText(getContext(), getString(R.string.graph_reloaded), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), getString(R.string.disabled_in_settings), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_reset:
                if (sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), getResources().getBoolean(R.bool.pref_graph_enabled_default))) {
                    showResetDialog();
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
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Contract.BROADCAST_STATUS_CHANGED);
        filter.addAction(Contract.BROADCAST_ON_OFF_CHANGED);
        getActivity().registerReceiver(dbChangedReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Activity activity = getActivity();
        if (activity != null) {
            activity.unregisterReceiver(dbChangedReceiver);
        }
        sharedPreferences.edit()
                .putBoolean(getString(R.string.pref_checkBox_percent), checkBox_percentage.isChecked())
                .putBoolean(getString(R.string.pref_checkBox_temperature), checkBox_temp.isChecked())
                .apply();
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

    @Override
    protected void loadSeries() {
        super.loadSeries();
        setTimeText();
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
                textView_chargingTime.setText(String.format("%s (%s)", InfoObject.getZeroTimeString(getContext()), getString(R.string.charging)));
            } else {
                textView_chargingTime.setText(getString(R.string.no_data));
            }
        }
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
}
