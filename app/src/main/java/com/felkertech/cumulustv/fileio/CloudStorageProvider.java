package com.felkertech.cumulustv.fileio;

import android.app.Activity;
import android.util.Log;

import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.n.cumulustv.R;
import com.felkertech.settingsmanager.SettingsManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

/**
 * Created by Nick on 12/15/2016.
 */

public class CloudStorageProvider {
    private static final String TAG = CloudStorageProvider.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static CloudStorageProvider mCloudStorageProvider;
    private GoogleApiClient mGoogleApiClient;
    private CloudProviderCallback mOnConnectedCallback;

    private CloudStorageProvider() {
    }

    public static CloudStorageProvider getInstance() {
        if (mCloudStorageProvider == null) {
            mCloudStorageProvider = new CloudStorageProvider();
        }
        return mCloudStorageProvider;
    }

    public GoogleApiClient connect(Activity activity) {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) activity)
                    .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener)
                            activity)
                    .build();
            mGoogleApiClient.connect();
        }
        return mGoogleApiClient;
    }

    public boolean autoConnect(Activity activity) {
        if(isDriveEnabled(activity)) {
            if (DEBUG) {
                Log.d(TAG, "Drive is enabled, automatically connect");
                Log.d(TAG, ">" + new SettingsManager(activity).getString(R.string.sm_google_drive_id).length());
                Log.d(TAG, new SettingsManager(activity).getString(R.string.sm_google_drive_id) + "<");
            }
            connect(activity);
            return true;
        }
        if (DEBUG) {
            Log.d(TAG, "Drive is not enabled, don't connect yet.");
        }
        return false;
    }

    public boolean isDriveEnabled(Activity activity) {
        String gdriveId = new SettingsManager(activity).getString(R.string.sm_google_drive_id);
        return gdriveId.isEmpty() && gdriveId.length() > 0;
    }

    public boolean isDriveConnected() {
        return mGoogleApiClient.isConnected();
    }

    public void pickDriveFile(final Activity activity) {
        if (!isDriveConnected()) {
            connect(activity);
            mOnConnectedCallback = new CloudProviderCallback() {
                @Override
                public void actionCompleted(int status) {
                    if (status == Activity.RESULT_OK) {
                        ActivityUtils.syncFile(activity, mGoogleApiClient);
                    }
                }
            };
        } else {
            ActivityUtils.syncFile(activity, mGoogleApiClient);
        }
    }

    public void switchFile(final Activity activity) {
        if (!isDriveConnected()) {
            connect(activity);
            mOnConnectedCallback = new CloudProviderCallback() {
                @Override
                public void actionCompleted(int status) {
                    if (status == Activity.RESULT_OK) {
                        ActivityUtils.switchGoogleDrive(activity, mGoogleApiClient);
                    }
                }
            };
        } else {
            ActivityUtils.switchGoogleDrive(activity, mGoogleApiClient);
        }
    }

    public void onConnected(int status) {
        if (mOnConnectedCallback != null) {
            mOnConnectedCallback.actionCompleted(status);
        }
    }

    public interface CloudProviderCallback {
        void actionCompleted(int status);
    }
}