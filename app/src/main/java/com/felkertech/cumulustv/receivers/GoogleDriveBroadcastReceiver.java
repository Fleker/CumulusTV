package com.felkertech.cumulustv.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Receives events related to the current Google Drive operation
 */
public abstract class GoogleDriveBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = GoogleDriveBroadcastReceiver.class.getSimpleName();

    public static final String ACTION_STATUS_CHANGED =
            GoogleDriveBroadcastReceiver.class.getPackage().getName() + ".status_changed";
    public static final String EXTRA_STATUS = "extra_status";
    public static final String EVENT_UPLOAD_COMPLETE =
            GoogleDriveBroadcastReceiver.class.getPackage().getName() + ".upload_complete";
    public static final String EVENT_DOWNLOAD_COMPLETE =
            GoogleDriveBroadcastReceiver.class.getPackage().getName() + ".download_complete";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_STATUS)) {
            Log.d(TAG, "Received intent " + intent.toString());
            if (intent.getStringExtra(EXTRA_STATUS).equals(EVENT_DOWNLOAD_COMPLETE)) {
                onNetworkActionCompleted();
                onDownloadCompleted();
            }
            if (intent.getStringExtra(EXTRA_STATUS).equals(EVENT_UPLOAD_COMPLETE)) {
                onNetworkActionCompleted();
                onUploadCompleted();
            }
        } else {
            Log.w(TAG, "Received invalid call");
        }
    }

    public static void changeStatus(Context context, String event) {
        Intent statusChangedEvent = new Intent(ACTION_STATUS_CHANGED);
//        statusChangedEvent.setAction();
        statusChangedEvent.putExtra(EXTRA_STATUS, event);
        Log.d(TAG, "Sending GDrive broadcast: " + event);
        Log.d(TAG, statusChangedEvent.toString());
        LocalBroadcastManager.getInstance(context).sendBroadcast(statusChangedEvent);
        context.sendBroadcast(statusChangedEvent);
    }

    public abstract void onDownloadCompleted();
    public abstract void onUploadCompleted();
    public abstract void onNetworkActionCompleted();
}
