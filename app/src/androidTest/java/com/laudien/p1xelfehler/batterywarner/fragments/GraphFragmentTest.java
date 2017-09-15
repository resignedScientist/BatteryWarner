package com.laudien.p1xelfehler.batterywarner.fragments;

import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.jjoe64.graphview.series.DataPoint;
import com.laudien.p1xelfehler.batterywarner.FragmentUtilActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static com.laudien.p1xelfehler.batterywarner.database.DatabaseController.NUMBER_OF_GRAPHS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GraphFragmentTest {
    @Rule
    public ActivityTestRule<FragmentUtilActivity> mActivityRule = new ActivityTestRule<>(
            FragmentUtilActivity.class);
    private GraphFragment graphFragment;

    @Before
    public void setup() {
        graphFragment = new GraphFragment();
        mActivityRule.getActivity().getSupportFragmentManager().beginTransaction()
                .add(mActivityRule.getActivity().layoutId, graphFragment, null)
                .commit();
        mActivityRule.getActivity().getSupportFragmentManager().executePendingTransactions();
        assertNotNull(graphFragment.getContext());
    }

    @Test
    @UiThreadTest
    public void onValueAddedTest() {
        DataPoint[] dataPoints = new DataPoint[NUMBER_OF_GRAPHS];
        Random random = new Random();

        // graphs == null and each dataPoint is null -> graphs should keep being null
        graphFragment.graphs = null;
        graphFragment.onValueAdded(dataPoints, 4);
        assertNull(graphFragment.graphs);

        /*
        graphs == null and one of the dataPoints is not null
        --> graphs should not be null
         */
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            graphFragment.graphs = null;
            dataPoints = new DataPoint[NUMBER_OF_GRAPHS];
            dataPoints[i] = new DataPoint(random.nextDouble(), random.nextDouble());
            graphFragment.onValueAdded(dataPoints, 4);
            assertNotNull(graphFragment.graphs);
            assertEquals(dataPoints[i].getX(), graphFragment.graphs[i].getHighestValueX(), 0d);
            assertEquals(dataPoints[i].getY(), graphFragment.graphs[i].getHighestValueY(), 0d);
        }

        // check with valid random data
        graphFragment.graphs = null;
        dataPoints = getRandomDataPoints(random);
        graphFragment.onValueAdded(dataPoints, 5);
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            assertEquals(dataPoints[i].getX(), graphFragment.graphs[i].getHighestValueX(), 0d);
            assertEquals(dataPoints[i].getY(), graphFragment.graphs[i].getHighestValueY(), 0d);
        }
    }

    private DataPoint[] getRandomDataPoints(Random random) {
        DataPoint[] dataPoints = new DataPoint[NUMBER_OF_GRAPHS];
        for (int i = 0; i < NUMBER_OF_GRAPHS; i++) {
            dataPoints[i] = new DataPoint(System.currentTimeMillis(), random.nextDouble());
        }
        return dataPoints;
    }
}
