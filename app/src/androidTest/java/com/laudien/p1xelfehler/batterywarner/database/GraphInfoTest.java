package com.laudien.p1xelfehler.batterywarner.database;

import android.app.Dialog;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.widget.TextView;

import com.laudien.p1xelfehler.batterywarner.R;

import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.util.Locale;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GraphInfoTest {
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
    }

    @Test
    @UiThreadTest
    public void dialogTest() {
        // test dialog text with first constructor, celsius and without current revert
        long startTime = System.currentTimeMillis();
        GraphInfo graphInfo = new GraphInfo(
                startTime,
                startTime + 120000,
                2d,
                243,
                210,
                24.3,
                -100000,
                -200000,
                3100,
                3800,
                80,
                20,
                false,
                false
        );

        // prepare data
        double minCurrent = DatabaseValue.convertToMilliAmperes(graphInfo.minCurrent, false);
        double maxCurrent = DatabaseValue.convertToMilliAmperes(graphInfo.maxCurrent, false);

        Dialog dialog = graphInfo.buildDialog(context);
        graphInfo.dialog = dialog;
        graphInfo.updateDialog(context);
        TextView startTimeText = dialog.findViewById(R.id.textView_startTime);
        TextView endTimeText = dialog.findViewById(R.id.textView_endTime);
        TextView minTempText = dialog.findViewById(R.id.textView_minTemp);
        TextView maxTempText = dialog.findViewById(R.id.textView_maxTemp);
        TextView minCurrentText = dialog.findViewById(R.id.textView_minCurrent);
        TextView maxCurrentText = dialog.findViewById(R.id.textView_maxCurrent);
        TextView minVoltageText = dialog.findViewById(R.id.textView_minVoltage);
        TextView maxVoltageText = dialog.findViewById(R.id.textView_maxVoltage);
        TextView chargingSpeedText = dialog.findViewById(R.id.textView_speed);
        TextView chargingTimeText = dialog.findViewById(R.id.textView_totalTime);

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        String expectedStartTime = String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_startTime),
                dateFormat.format(startTime));
        String expectedEndTime = String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_endTime),
                dateFormat.format(graphInfo.endTime));
        String expectedMinTemp = String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_min_temp),
                DatabaseValue.getTemperatureString(graphInfo.minTemp, false));
        String expectedMaxTemp = String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_max_temp),
                DatabaseValue.getTemperatureString(graphInfo.maxTemp, false));
        String expectedMinCurrent = String.format(
                Locale.getDefault(),
                "%s: %.1f mAh",
                context.getString(R.string.info_min_current),
                minCurrent);
        String expectedMaxCurrent = String.format(
                Locale.getDefault(),
                "%s: %.1f mAh",
                context.getString(R.string.info_max_current),
                maxCurrent);
        String expectedMinVoltage = String.format(
                Locale.getDefault(),
                "%s: %.3f V",
                context.getString(R.string.info_min_voltage),
                graphInfo.minVoltage);
        String expectedMaxVoltage = String.format(
                Locale.getDefault(),
                "%s: %.3f V",
                context.getString(R.string.info_max_voltage),
                graphInfo.maxVoltage);
        String expectedChargingSpeed = String.format(
                Locale.getDefault(),
                "%s: %.2f %%/h",
                context.getString(R.string.info_charging_speed),
                graphInfo.chargingSpeed);
        String expectedChargingTime = String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_charging_time),
                graphInfo.getTimeString(context));

        // test data
        assertEquals(expectedStartTime, startTimeText.getText().toString());
        assertEquals(expectedEndTime, endTimeText.getText().toString());
        assertEquals(expectedMinTemp, minTempText.getText().toString());
        assertEquals(expectedMaxTemp, maxTempText.getText().toString());
        if (SDK_INT >= LOLLIPOP) {
            assertEquals(expectedMinCurrent, minCurrentText.getText().toString());
            assertEquals(expectedMaxCurrent, maxCurrentText.getText().toString());
        } else {
            assertNull(minCurrentText);
            assertNull(maxCurrentText);
        }
        assertEquals(expectedMinVoltage, minVoltageText.getText().toString());
        assertEquals(expectedMaxVoltage, maxVoltageText.getText().toString());
        assertEquals(expectedChargingSpeed, chargingSpeedText.getText().toString());
        assertEquals(expectedChargingTime, chargingTimeText.getText().toString());
    }

    @Test
    @UiThreadTest
    public void dialogTest2() {
        // Test with Fahrenheit
        long startTime = System.currentTimeMillis();
        GraphInfo graphInfo = new GraphInfo(
                startTime,
                startTime + 120000,
                2d,
                243,
                210,
                24.3,
                -100000,
                -200000,
                3100,
                3800,
                80,
                20,
                true,
                false
        );

        // prepare data
        Dialog dialog = graphInfo.buildDialog(context);
        graphInfo.dialog = dialog;
        graphInfo.updateDialog(context);

        TextView minTempText = dialog.findViewById(R.id.textView_minTemp);
        TextView maxTempText = dialog.findViewById(R.id.textView_maxTemp);

        String expectedMinTemp = String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_min_temp),
                DatabaseValue.getTemperatureString(210, true));
        String expectedMaxTemp = String.format(
                Locale.getDefault(),
                "%s: %s",
                context.getString(R.string.info_max_temp),
                DatabaseValue.getTemperatureString(243, true));

        // test data
        assertEquals(expectedMinTemp, minTempText.getText().toString());
        assertEquals(expectedMaxTemp, maxTempText.getText().toString());
    }

    @Test
    @UiThreadTest
    public void dialogTest3() {
        // Test with revert current
        long startTime = System.currentTimeMillis();
        GraphInfo graphInfo = new GraphInfo(
                startTime,
                startTime + 120000,
                2d,
                243,
                210,
                24.3,
                -100000,
                -200000,
                3100,
                3800,
                80,
                20,
                false,
                true
        );

        // prepare data
        double minCurrent = DatabaseValue.convertToMilliAmperes(graphInfo.minCurrent, true);
        double maxCurrent = DatabaseValue.convertToMilliAmperes(graphInfo.maxCurrent, true);

        Dialog dialog = graphInfo.buildDialog(context);
        graphInfo.dialog = dialog;
        graphInfo.updateDialog(context);

        TextView minCurrentText = dialog.findViewById(R.id.textView_minCurrent);
        TextView maxCurrentText = dialog.findViewById(R.id.textView_maxCurrent);

        String expectedMinCurrent = String.format(
                Locale.getDefault(),
                "%s: %.1f mAh",
                context.getString(R.string.info_min_current),
                minCurrent);
        String expectedMaxCurrent = String.format(
                Locale.getDefault(),
                "%s: %.1f mAh",
                context.getString(R.string.info_max_current),
                maxCurrent);

        // test data
        if (SDK_INT >= LOLLIPOP) {
            assertEquals(expectedMinCurrent, minCurrentText.getText().toString());
            assertEquals(expectedMaxCurrent, maxCurrentText.getText().toString());
        } else {
            assertNull(minCurrentText);
            assertNull(maxCurrentText);
        }
    }

    @Test
    public void secondConstructorTest() {
        // test the second constructor
        long timeNow = System.currentTimeMillis();
        DatabaseValue inputValue = new DatabaseValue(20, 234, 5000, -1200000, timeNow, timeNow);

        // useFahrenheit = false, reverseCurrent = false
        GraphInfo graphInfo = new GraphInfo(inputValue, false, false);
        assertEquals(inputValue.getTemperature(), graphInfo.minTemp);
        assertEquals(inputValue.getTemperature(), graphInfo.maxTemp);
        assertEquals(inputValue.getUtcTimeInMillis(), graphInfo.startTime);
        assertEquals(inputValue.getUtcTimeInMillis(), graphInfo.endTime);
        assertEquals(false, graphInfo.useFahrenheit);
        assertEquals(false, graphInfo.reverseCurrent);
        assertEquals(inputValue.getBatteryLevel(), graphInfo.firstBatteryLvl);
        assertEquals(inputValue.getBatteryLevel(), graphInfo.maxBatteryLvl);
        assertEquals(inputValue.getTimeFromStartInMinutes(), graphInfo.timeInMinutes, 0d);
        assertEquals(inputValue.getTemperature(), graphInfo.maxTemp);
        assertEquals(inputValue.getTemperature(), graphInfo.minTemp);
        int expectedCurrent = SDK_INT >= LOLLIPOP ? inputValue.getCurrent() : 0;
        assertEquals(expectedCurrent, graphInfo.minCurrent);
        assertEquals(expectedCurrent, graphInfo.maxCurrent);
        assertEquals(inputValue.getTimeFromStartInMinutes(), graphInfo.timeInMinutes, 0d);
        assertEquals(inputValue.getVoltageInVolts(), graphInfo.minVoltage, 0d);
        assertEquals(inputValue.getVoltageInVolts(), graphInfo.maxVoltage, 0d);
        assertEquals(Double.NaN, graphInfo.chargingSpeed, 0d);

        // useFahrenheit = false, reverseCurrent = true
        graphInfo = new GraphInfo(inputValue, false, true);
        assertEquals(false, graphInfo.useFahrenheit);
        assertEquals(true, graphInfo.reverseCurrent);

        // useFahrenheit = true, reverseCurrent = false
        graphInfo = new GraphInfo(inputValue, true, false);
        assertEquals(true, graphInfo.useFahrenheit);
        assertEquals(false, graphInfo.reverseCurrent);

        // useFahrenheit = true, reverseCurrent = true
        graphInfo = new GraphInfo(inputValue, true, true);
        assertEquals(true, graphInfo.useFahrenheit);
        assertEquals(true, graphInfo.reverseCurrent);
    }

    @Test
    public void notifyValueAddedTest() {
        long timeNow = System.currentTimeMillis();

        DatabaseValue firstValue = new DatabaseValue(25, 280, 5000, -1190000, timeNow, timeNow);
        DatabaseValue[] values = new DatabaseValue[]{
                new DatabaseValue(20, 234, 5000, -1200000, timeNow, timeNow + 1000000),
                new DatabaseValue(50, 300, 5100, -1210000, timeNow + 2000000, timeNow),
                new DatabaseValue(30, 250, 4900, -1000000, timeNow + 3000000, timeNow)
        };

        GraphInfo graphInfo = new GraphInfo(firstValue, false, false);

        // first value
        assertEquals(firstValue.getBatteryLevel(), graphInfo.maxBatteryLvl);
        assertEquals(firstValue.getBatteryLevel(), graphInfo.firstBatteryLvl);
        assertEquals(firstValue.getTemperature(), graphInfo.minTemp);
        assertEquals(firstValue.getTemperature(), graphInfo.maxTemp);
        assertEquals(firstValue.getVoltageInVolts(), graphInfo.minVoltage, 0d);
        assertEquals(firstValue.getVoltageInVolts(), graphInfo.maxVoltage, 0d);
        int expectedCurrent = SDK_INT >= LOLLIPOP ? firstValue.getCurrent() : 0;
        assertEquals(expectedCurrent, graphInfo.minCurrent);
        assertEquals(expectedCurrent, graphInfo.maxCurrent);
        assertEquals(firstValue.getTimeFromStartInMinutes(), graphInfo.timeInMinutes, 0d);
        assertEquals(firstValue.getUtcTimeInMillis(), graphInfo.startTime);
        assertEquals(firstValue.getUtcTimeInMillis(), graphInfo.endTime);

        // second value
        graphInfo.notifyValueAdded(values[0], context);
        assertEquals(firstValue.getBatteryLevel(), graphInfo.maxBatteryLvl);
        assertEquals(firstValue.getBatteryLevel(), graphInfo.firstBatteryLvl);
        assertEquals(values[0].getTemperature(), graphInfo.minTemp);
        assertEquals(firstValue.getTemperature(), graphInfo.maxTemp);
        assertEquals(firstValue.getVoltageInVolts(), graphInfo.minVoltage, 0d);
        assertEquals(firstValue.getVoltageInVolts(), graphInfo.maxVoltage, 0d);
        int expectedMinCurrent = SDK_INT >= LOLLIPOP ? firstValue.getCurrent() : 0;
        int expectedMaxCurrent = SDK_INT >= LOLLIPOP ? values[0].getCurrent() : 0;
        assertEquals(expectedMinCurrent, graphInfo.minCurrent);
        assertEquals(expectedMaxCurrent, graphInfo.maxCurrent);
        assertEquals(values[0].getTimeFromStartInMinutes(), graphInfo.timeInMinutes, 0d);
        assertEquals(firstValue.getUtcTimeInMillis(), graphInfo.startTime);
        assertEquals(values[0].getUtcTimeInMillis(), graphInfo.endTime);
    }
}