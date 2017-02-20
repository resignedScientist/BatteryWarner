package com.laudien.p1xelfehler.batterywarner.Activities.MainActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity.HistoryActivity;
import com.laudien.p1xelfehler.batterywarner.Activities.InfoObject;
import com.laudien.p1xelfehler.batterywarner.BatteryAlarmManager;
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

public class GraphFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "GraphFragment";
    private static final int REQUEST_SAVE_GRAPH = 10;
    private static final int REQUEST_LOAD_GRAPH = 20;
    private SharedPreferences sharedPreferences;
    private GraphView graph_chargeCurve;
    private LineGraphSeries<DataPoint> series_chargeCurve, series_temp;
    private Viewport viewport_chargeCurve;
    private TextView textView_chargingTime;
    private CheckBox checkBox_percentage, checkBox_temp;
    private int graphCounter;
    private boolean graphEnabled;
    private InfoObject infoObject;
    private long endTime;
    private BroadcastReceiver dbChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadChargeCurve();
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
        // return if the database has not enough data
        if (!GraphDbHelper.getInstance(context).hasEnoughData()) {
            return;
        }
        BatteryAlarmManager batteryAlarmManager = BatteryAlarmManager.getInstance(context);
        if (!batteryAlarmManager.isGraphEnabled()) {
            return; // return if graph is disabled in settings
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
        String inputFileDir = String.format(
                Locale.getDefault(),
                "/data/data/%s/databases/%s",
                Contract.PACKAGE_NAME_PRO,
                GraphDbHelper.DATABASE_NAME
        );
        File inputFile = new File(inputFileDir);
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
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        graph_chargeCurve = (GraphView) view.findViewById(R.id.graph_chargeCurve);
        viewport_chargeCurve = graph_chargeCurve.getViewport();
        textView_chargingTime = (TextView) view.findViewById(R.id.textView_chargingTime);
        graphEnabled = sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), true);

        // checkBoxes
        checkBox_percentage = (CheckBox) view.findViewById(R.id.checkbox_percentage);
        checkBox_temp = (CheckBox) view.findViewById(R.id.checkBox_temp);

        if (!graphEnabled) { // disable checkboxes if the graph is disabled
            checkBox_percentage.setEnabled(false);
            checkBox_temp.setEnabled(false);
        } else {
            Context context = getContext();
            checkBox_percentage.setChecked(sharedPreferences.getBoolean(context.getString(R.string.pref_checkBox_percent), true));
            checkBox_temp.setChecked(sharedPreferences.getBoolean(context.getString(R.string.pref_checkBox_temperature), false));
            checkBox_percentage.setOnCheckedChangeListener(this);
            checkBox_temp.setOnCheckedChangeListener(this);
        }

        // y bounds
        viewport_chargeCurve.setYAxisBoundsManual(true);
        viewport_chargeCurve.setMinY(0);
        viewport_chargeCurve.setMaxY(100);

        // x bounds
        viewport_chargeCurve.setXAxisBoundsManual(true);
        viewport_chargeCurve.setMinX(0);

        graph_chargeCurve.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) { // X-axis (time)
                    if (value == 0) {
                        graphCounter = 1;
                        return "0 min";
                    }
                    if (value < 0.1) {
                        return "";
                    }
                    if (graphCounter++ % 3 == 0)
                        return super.formatLabel(value, true) + " min";
                    return "";
                } else if (checkBox_percentage.isChecked() ^ checkBox_temp.isChecked()) { // Y-axis (percent)
                    if (checkBox_percentage.isChecked())
                        return super.formatLabel(value, false) + "%";
                    if (checkBox_temp.isChecked())
                        return super.formatLabel(value, false) + "°C";
                }
                return super.formatLabel(value, false);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadChargeCurve();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Contract.BROADCAST_STATUS_CHANGED);
        filter.addAction(Contract.BROADCAST_ON_OFF_CHANGED);
        getActivity().registerReceiver(dbChangedReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Context context = getContext();
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_checkBox_percent), checkBox_percentage.isChecked())
                .putBoolean(context.getString(R.string.pref_checkBox_temperature), checkBox_temp.isChecked())
                .apply();
        getActivity().unregisterReceiver(dbChangedReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.reload_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (!Contract.IS_PRO && id != R.id.menu_open_history) {
            Toast.makeText(getContext(), getString(R.string.pro_only_short), Toast.LENGTH_SHORT).show();
            return false;
        }
        switch (id) {
            case R.id.menu_refresh:
                if (graphEnabled) {
                    reloadChargeCurve();
                    Toast.makeText(getContext(), getString(R.string.graph_reloaded), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), getString(R.string.disabled_in_settings), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_reset:
                if (sharedPreferences.getBoolean(getString(R.string.pref_graph_enabled), true)) {
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
                if (infoObject != null) {
                    infoObject.showDialog(getActivity());
                } else {
                    Toast.makeText(getContext(), getString(R.string.no_data), Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        LineGraphSeries series = null;
        switch (compoundButton.getId()) {
            case R.id.checkbox_percentage:
                series = series_chargeCurve;
                break;
            case R.id.checkBox_temp:
                series = series_temp;
                break;
        }
        if (series == null) return;
        if (b)
            graph_chargeCurve.addSeries(series);
        else
            graph_chargeCurve.removeSeries(series);
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
        } else if (requestCode == REQUEST_LOAD_GRAPH) {
            openHistory();
        }
    }

    private void openHistory() {
        // check for permission
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    REQUEST_LOAD_GRAPH
            );
            return;
        }
        startActivity(new Intent(getContext(), HistoryActivity.class));
    }

    private void saveGraph() {
        // check if a graph is present and has enough data
        if (graph_chargeCurve.getSeries().size() == 0 || series_chargeCurve.getHighestValueX() == 0) {
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

    public void reloadChargeCurve() {
        // if not pro -> return
        if (!Contract.IS_PRO) {
            textView_chargingTime.setTextSize(20);
            textView_chargingTime.setText(getString(R.string.not_pro));
            checkBox_temp.setEnabled(false);
            checkBox_percentage.setEnabled(false);
            return;
        }
        // if graph disabled in settings -> return
        if (!graphEnabled) {
            textView_chargingTime.setTextSize(18);
            textView_chargingTime.setText(getString(R.string.disabled_in_settings));
            return;
        }
        // remove the series from the graph view
        graph_chargeCurve.removeSeries(series_chargeCurve);
        graph_chargeCurve.removeSeries(series_temp);
        // load graph
        Intent batteryStatus = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus == null) return;
        boolean isChargingAndNotFull = BatteryAlarmManager.isChargingNotificationEnabled(getContext(), sharedPreferences)
                && batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, Contract.NO_STATE) != 100
                && sharedPreferences.getBoolean(getContext().getString(R.string.pref_is_enabled), true);
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        LineGraphSeries<DataPoint> series[] = dbHelper.getGraphs(getContext());
        if (series != null) {
            series_chargeCurve = series[GraphDbHelper.TYPE_PERCENTAGE];
            series_temp = series[GraphDbHelper.TYPE_TEMPERATURE];
            if (checkBox_percentage.isChecked()) {
                graph_chargeCurve.addSeries(series_chargeCurve);
            }
            if (checkBox_temp.isChecked()) {
                graph_chargeCurve.addSeries(series_temp);
            }
            updateInfoObject();
            String timeString = infoObject.getTimeString(getContext());
            if (infoObject.getTimeInMinutes() != 0) { // enough data
                if (isChargingAndNotFull) {
                    textView_chargingTime.setText(String.format("%s (%s)", getString(R.string.charging), timeString));
                } else {
                    textView_chargingTime.setText(String.format("%s: %s", getString(R.string.charging_time), timeString));
                }
                viewport_chargeCurve.setMaxX(infoObject.getTimeInMinutes());
            } else { // not enough data
                viewport_chargeCurve.setMaxX(1.0);
                if (isChargingAndNotFull) {
                    textView_chargingTime.setText(String.format("%s (%s)", getString(R.string.charging), InfoObject.getZeroTimeString(getContext())));
                } else {
                    textView_chargingTime.setText(getString(R.string.not_enough_data));
                }
            }
        } else { // empty database
            viewport_chargeCurve.setMaxX(1.0);
            if (isChargingAndNotFull) {
                textView_chargingTime.setText(String.format("%s (%s)", InfoObject.getZeroTimeString(getContext()), getString(R.string.charging)));
            } else {
                textView_chargingTime.setText(getString(R.string.no_data));
            }
        }
    }

    private void updateInfoObject() {
        GraphDbHelper dbHelper = GraphDbHelper.getInstance(getContext());
        if (infoObject == null) {
            endTime = GraphDbHelper.getEndTime(dbHelper.getReadableDatabase());
            infoObject = new InfoObject(
                    endTime,
                    series_chargeCurve.getHighestValueX(),
                    series_temp.getHighestValueY(),
                    series_temp.getLowestValueY(),
                    series_chargeCurve.getHighestValueY() - series_chargeCurve.getLowestValueY()
            );
        } else {
            infoObject.updateValues(
                    endTime,
                    series_chargeCurve.getHighestValueX(),
                    series_temp.getHighestValueY(),
                    series_temp.getLowestValueY(),
                    series_chargeCurve.getHighestValueY() - series_chargeCurve.getLowestValueY()
            );
        }
    }

    private void showResetDialog() {
        new AlertDialog.Builder(getContext())
                .setCancelable(true)
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
}
