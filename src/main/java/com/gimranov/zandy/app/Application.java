package com.gimranov.zandy.app;

/**
 * Part of the Scopely™ Platform
 * © 2013 Scopely, Inc.
 */
public class Application extends android.app.Application {
    private static final String TAG = Application.class.getCanonicalName();

    private static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }

    public static Application getInstance() {
        return instance;
    }
}
