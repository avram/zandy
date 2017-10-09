package com.gimranov.zandy.app.storage;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class StorageManager {
    public static File getDocumentsDirectory(Context context) {
        File documents = new File(context.getExternalFilesDir(null), "documents");
        //noinspection ResultOfMethodCallIgnored
        documents.mkdirs();
        return documents;
    }

    public static File getCacheDirectory(Context context) {
        File cache = new File(context.getExternalFilesDir(null), "cache");
        //noinspection ResultOfMethodCallIgnored
        cache.mkdirs();
        return cache;
    }
}
