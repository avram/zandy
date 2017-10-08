package com.gimranov.zandy.app;

class SyncEvent {
    private static final String TAG = SyncEvent.class.getCanonicalName();

    static final int COMPLETE_CODE = 1;

    static final SyncEvent COMPLETE = new SyncEvent(COMPLETE_CODE);

    private int status;

    private SyncEvent(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
