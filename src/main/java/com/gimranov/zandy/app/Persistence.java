package com.gimranov.zandy.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

class Persistence {

    private static final String FILE = "Persistence";

    static void write(String key, String value) {
        SharedPreferences.Editor editor = Application.getInstance().getSharedPreferences(FILE, Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    @Nullable
    static String read(String key) {
        SharedPreferences store = Application.getInstance().getSharedPreferences(FILE, Context.MODE_PRIVATE);
        if (!store.contains(key)) return null;

        return store.getString(key, null);
    }
}
