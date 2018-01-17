package com.laudien.p1xelfehler.batterywarner.database;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class Data {
    private LineGraphSeries<DataPoint>[] graphs;
    private GraphInfo graphInfo;

    Data(LineGraphSeries<DataPoint>[] graphs, GraphInfo graphInfo) {
        this.graphs = graphs;
        this.graphInfo = graphInfo;
    }

    public LineGraphSeries<DataPoint>[] getGraphs() {
        return graphs;
    }

    public GraphInfo getGraphInfo() {
        return graphInfo;
    }
}
