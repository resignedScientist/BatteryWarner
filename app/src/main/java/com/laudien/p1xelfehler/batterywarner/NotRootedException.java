package com.laudien.p1xelfehler.batterywarner;

public class NotRootedException extends Exception {
    public NotRootedException() {
        super("The device is not rooted!");
    }
}
