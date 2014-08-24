package com.gimranov.zandy.app;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.zxing.integration.android.IntentIntegrator;

public class AmazonZxingGlue {
    private static final String TAG = AmazonZxingGlue.class.getCanonicalName();

    public static AlertDialog showDownloadDialog(final Context activity) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle(IntentIntegrator.DEFAULT_TITLE);
        downloadDialog.setMessage(IntentIntegrator.DEFAULT_MESSAGE);
        downloadDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("amzn://apps/android?asin=B004R1FCII");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {
                    // Hmm, market is not installed
                    Log.w(TAG, "Amazon is not installed; cannot install");
                }
            }
        });
        downloadDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });
        return downloadDialog.show();
    }
}
