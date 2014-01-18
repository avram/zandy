package com.gimranov.zandy.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.jetbrains.annotations.Nullable;

/**
 * Part of the Scopely™ Platform
 * © 2013 Scopely, Inc.
 */
public class Persistence {
    private static final String TAG = Persistence.class.getCanonicalName();

    private static final String FILE = "Persistence";

    public static void write(String key, String value) {
        SharedPreferences.Editor editor = Application.getInstance().getSharedPreferences(FILE, Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.commit();
    }

    @Nullable
    public static String read(String key) {
        SharedPreferences store = Application.getInstance().getSharedPreferences(FILE, Context.MODE_PRIVATE);
        if (!store.contains(key)) return null;

        return store.getString(key, null);
    }
}
