package com.felkertech.cumulustv.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives events related to the current Google Drive operation
 */
public abstract class GoogleDriveBroadcastReceiver extends BroadcastReceiver {
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
            if (intent.getStringExtra(EXTRA_STATUS).equals(EVENT_DOWNLOAD_COMPLETE)) {
                onNetworkActionCompleted();
                onDownloadCompleted();
            }
            if (intent.getStringExtra(EXTRA_STATUS).equals(EVENT_UPLOAD_COMPLETE)) {
                onNetworkActionCompleted();
                onUploadCompleted();
            }
        }
    }

    public static void changeStatus(Context context, String event) {
        Intent statusChangedEvent = new Intent();
        statusChangedEvent.setAction(ACTION_STATUS_CHANGED);
        statusChangedEvent.putExtra(EXTRA_STATUS, event);
        context.sendBroadcast(statusChangedEvent);
    }

    public abstract void onDownloadCompleted();
    public abstract void onUploadCompleted();
    public abstract void onNetworkActionCompleted();
}
