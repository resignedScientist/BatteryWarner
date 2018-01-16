package com.laudien.p1xelfehler.batterywarner.database;

public class Data {
    private DatabaseValue[] databaseValues;
    private GraphInfo graphInfo;

    Data(DatabaseValue[] databaseValues, GraphInfo graphInfo) {
        this.databaseValues = databaseValues;
        this.graphInfo = graphInfo;
    }

    public DatabaseValue[] getDatabaseValues() {
        return databaseValues;
    }

    public GraphInfo getGraphInfo() {
        return graphInfo;
    }
}
