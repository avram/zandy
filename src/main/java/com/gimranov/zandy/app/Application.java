package com.gimranov.zandy.app;

import com.squareup.otto.Bus;

/**
 * Part of the Scopely™ Platform
 * © 2013 Scopely, Inc.
 */
public class Application extends android.app.Application {
    private static final String TAG = Application.class.getCanonicalName();

    private static Application instance;

    private Bus bus;

    @Override
    public void onCreate() {
        super.onCreate();

        bus = new Bus();

        instance = this;
    }

    public Bus getBus() {
        return bus;
    }

    public static Application getInstance() {
        return instance;
    }
}
