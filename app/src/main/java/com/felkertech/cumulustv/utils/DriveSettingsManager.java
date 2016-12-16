package com.felkertech.cumulustv.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.felkertech.settingsmanager.SettingsManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by Nick on 3/22/2016.
 */
public class DriveSettingsManager extends SettingsManager {
    private static final String TAG = DriveSettingsManager.class.getSimpleName();
    private static final boolean DEBUG = false;

    private GoogleApiClient mGoogleApiClient;
    private GoogleDriveListener mGoogleDriveListener;

    public DriveSettingsManager(Context context) {
        super(context);
    }

    /* GOOGLE DRIVE */
    public void setGoogleDriveSyncable(GoogleApiClient gapi) {
        this.mGoogleApiClient = gapi;
    }

    public void setGoogleDriveSyncable(GoogleApiClient gapi, GoogleDriveListener gdl) {
        this.mGoogleApiClient = gapi;
        this.mGoogleDriveListener = gdl;
    }

    /**
     * Writes some data to a Google Drive file, this can get from a preference
     * @param driveId The id of the file to be written to
     * @param data The string of data that should be written
     */
    public void writeToGoogleDrive(DriveId driveId, String data) {
        DriveFile file = Drive.DriveApi.getFile(mGoogleApiClient, driveId);
        if (DEBUG) {
            Log.d(TAG, "Writing to " + driveId + " -> " + data);
        }
        new EditGoogleDriveAsyncTask(mContext).execute(file, data);
    }

    public void readFromGoogleDrive(DriveId driveId, final int resId) {
        readFromGoogleDrive(driveId, mContext.getString(resId));
    }

    /**
     * Reads a file from Google Drive and inserts that data into a preference
     * @param driveId The id of the file to be read from
     * @param resId The key for the desired preference
     */
    public void readFromGoogleDrive(DriveId driveId, final String resId) {
        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            return;
        }
        ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
                new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            if (DEBUG) {
                                Log.d(TAG, "Error while opening the file contents");
                            }
                            return;
                        }
                        if (DEBUG) {
                            Log.d(TAG, "File contents opened");
                        }
                        //Now step 2, build the contents
                        try {
                            DriveContents contents = result.getDriveContents();
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(contents.getInputStream()));
                            StringBuilder builder = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                builder.append(line);
                            }
                            String contentsAsString = builder.toString();
                            if (DEBUG) {
                                Log.d(TAG, "From " + contents.getDriveId());
                                Log.d(TAG, "Retrieved " + contentsAsString);
                            }
                            //Step 3, write to SM
                            setString(resId, contentsAsString);

                            //Step 4, close stream (or not? java.lang.IllegalStateException: Cannot commit contents opened with MODE_READ_ONLY)
                            /*contents.commit(mGoogleApiClient, null).setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(Status result) {
                                    // handle the response status
                                }
                            });*/

                            //Step 5, send alert
                            if(mGoogleDriveListener != null) {
                                Handler h = new Handler(Looper.getMainLooper()) {
                                    @Override
                                    public void handleMessage(Message msg) {
                                        super.handleMessage(msg);
                                        mGoogleDriveListener.onActionFinished(true);
                                    }
                                };
                                h.sendEmptyMessage(0);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };

        Drive.DriveApi.getFile(mGoogleApiClient, driveId)
                .open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                .setResultCallback(driveContentsCallback);
    }

    public class EditGoogleDriveAsyncTask extends AsyncTask<Object, Void, Boolean> {

        DriveContents driveContents;
        public EditGoogleDriveAsyncTask(Context context) {
            super();
        }

        @Override
        protected Boolean doInBackground(Object... args) {
            DriveFile file = (DriveFile) args[0];
            String data = (String) args[1];
            try {
                DriveApi.DriveContentsResult driveContentsResult = file.open(
                        mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    return false;
                }
                driveContents = driveContentsResult.getDriveContents();
                OutputStream outputStream = driveContents.getOutputStream();
                outputStream.write(data.getBytes());
                com.google.android.gms.common.api.Status status =
                        driveContents.commit(mGoogleApiClient, null).await();
                if(mGoogleDriveListener != null) {
                    Handler h = new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            mGoogleDriveListener.onActionFinished(false);
                        }
                    };
                    h.sendEmptyMessage(0);
                }
                return status.getStatus().isSuccess();
            } catch (IOException e) {
                if (DEBUG) {
                    Log.e(TAG, "IOException while appending to the output stream", e);
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (DEBUG) {
                Log.d(TAG, "EditContents result " + result);
            }
            /*driveContents.commit(mGoogleApiClient, null).setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
                @Override
                public void onResult(com.google.android.gms.common.api.Status result) {
                    // Handle the response status
                }
            });*/

        }
    }

    public interface GoogleDriveListener {
        void onActionFinished(boolean cloudToLocal);
    }
}
