package com.gimranov.zandy.app;

/**
 * Part of the Scopely™ Platform
 * © 2013 Scopely, Inc.
 */
public class SyncEvent {
    private static final String TAG = SyncEvent.class.getCanonicalName();

    public static final int COMPLETE_CODE = 1;

    public static final SyncEvent COMPLETE = new SyncEvent(COMPLETE_CODE);

    private int status;

    public SyncEvent(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
