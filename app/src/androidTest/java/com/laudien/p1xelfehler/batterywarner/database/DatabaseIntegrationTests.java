package com.laudien.p1xelfehler.batterywarner.database;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DatabaseIntegrationTests {
    @Before
    public void setUp() throws Exception {
        // TODO: (1) delete all files
        // TODO: (2) delete the current graph
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getFileList() throws Exception {
        // TODO: (1) create multiple graphs and save them
        // TODO: (2) check if getFileList() returns all the files that were saved
    }

    @Test
    public void getAllGraphs() throws Exception {
        // TODO: create multiple graphs and check if getAllGraphs() returns exactly what was added -> none of the values should be the same as before!
    }

    @Test
    public void getAllGraphs1() throws Exception {
        // TODO: (1) create multiple graphs and save them -> none of the values should be the same as before!
        // TODO: (2) check if the saved graphs were shortened correctly
    }

    @Test
    public void getAllGraphs2() throws Exception {
        // TODO: (1) add 20 same values
        // TODO: (2) verify that only 2 values are read
    }

    @Test
    public void getEndTime() throws Exception {
        // TODO: (1) add a few values
        // TODO: (2) verify that the last time equals the getEndTime()
    }

    @Test
    public void getEndTime1() throws Exception {
        // TODO: (1) add a few values
        // TODO: (2) save the graph
        // TODO: (3) verify that the last time equals the getEndTime()
    }

    @Test
    public void getStartTime() throws Exception {
        // TODO: (1) add a few values
        // TODO: (2) verify that the first time equals the getStartTime()
    }

    @Test
    public void getStartTime1() throws Exception {
        // TODO: (1) add a few values
        // TODO: (2) save the graph
        // TODO: (3) verify that the first time equals the getStartTime()
    }

    @Test
    public void saveGraph() throws Exception {
        // TODO: (1) add a few values
        // TODO: (2) save the graph
        // TODO: (3) verify that the file is there
        // TODO: (4) verify that the file has the correct name
    }

    @Test
    public void randomValueTest() throws Exception {
        // TODO: (1) add 1000 random values -> all must be different!
        // TODO: (2) read the values and compare them with the input
    }

    @Test
    public void upgradeAllSavedDatabases() throws Exception {
        // TODO: (1) create a graph database that has the old format
        // TODO: (2) upgrade this database
        // TODO: (3) verify the output database has the new format
    }

}