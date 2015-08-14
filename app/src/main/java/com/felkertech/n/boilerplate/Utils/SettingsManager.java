package com.felkertech.n.boilerplate.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
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
 * Version 1.1
 * Created by N on 14/9/2014.
 * Last Edited 13/5/2015
 *   * Support for syncing data to wearables
 */
public class SettingsManager {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String TAG = "PreferenceManager";
    private Context mContext;
    public SettingsManager(Activity activity) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        mContext = activity;
//        sharedPreferences = getDefaultSharedPreferences(activity);
//        sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
//        sharedPreferences = activity.getSharedPreferences(activity.getString(R.string.PREFERENCES), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
//        Log.d(TAG, sharedPreferences.getAll().keySet().iterator().next());
    }
    public SettingsManager(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;
        editor = sharedPreferences.edit();
    }
    public Context getContext() {
        return mContext;
    }
    public String getString(int resId) {
        return getString(mContext.getString(resId));
    }
    public String getString(String key) {
        return getString(key, "-1", "");
    }
    public String getString(int resId, String def) {
        return getString(mContext.getString(resId), def);
    }
    public String getString(String key, String def) {
        return getString(key, "-1", def);
    }
    public String getString(String key, String val, String def) {
//        Log.d(TAG, key + " - " + val + " - " + def);
        String result = sharedPreferences.getString(key, val);
        assert result != null;
        if(result.equals("-1")) {
            editor.putString(key, def);
            Log.d(TAG, key + ", " + def);
            editor.commit();
            result = def;
        }
        return result;
    }
    public String setString(int resId, String val) {
        return setString(mContext.getString(resId), val);
    }
    public String setString(String key, String val) {
        editor.putString(key, val);
        editor.commit();
        return val;
    }
    public boolean getBoolean(int resId) {
        return getBoolean(mContext.getString(resId));
    }
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }
    public boolean getBoolean(String key, boolean def) {
        boolean result = sharedPreferences.getBoolean(key, def);
        editor.putBoolean(key, result);
        editor.commit();
        return result;
    }
    public boolean setBoolean(int resId, boolean val) {
        return setBoolean(mContext.getString(resId), val);
    }
    public boolean setBoolean(String key, boolean val) {
        editor.putBoolean(key, val);
        editor.commit();
        return val;
    }

    public int getInt(int resId) {
        return sharedPreferences.getInt(mContext.getString(resId), 0);
    }
    public int setInt(int resId, int val) {
        return setInt(mContext.getString(resId), val);
    }
    public int setInt(String key, int val) {
        editor.putInt(key, val);
        editor.commit();
        return val;
    }
    public long getLong(int resId) {
        return sharedPreferences.getLong(mContext.getString(resId), 0);
    }
    public long setLong(int resId, long val) {
        return setLong(mContext.getString(resId), val);
    }
    public long setLong(String key, long val) {
        editor.putLong(key, val);
        editor.commit();
        return val;
    }

    //Default Stuff
    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context),
                getDefaultSharedPreferencesMode());
    }

    private static String getDefaultSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    private static int getDefaultSharedPreferencesMode() {
        return Context.MODE_PRIVATE;
    }

    /* GOOGLE DRIVE */
    private GoogleApiClient gapi;
    private GoogleDriveListener gdl;
    public void setGoogleDriveSyncable(GoogleApiClient gapi) {
        this.gapi = gapi;
    }
    public void setGoogleDriveSyncable(GoogleApiClient gapi, GoogleDriveListener gdl) {
        this.gapi = gapi;
        this.gdl = gdl;
    }

    /**
     * Writes some data to a Google Drive file, this can get from a preference
     * @param driveId The id of the file to be written to
     * @param data The string of data that should be written
     */
    public void writeToGoogleDrive(DriveId driveId, String data) {
        DriveFile file = Drive.DriveApi.getFile(gapi, driveId);
        Log.d(TAG, "Writing to "+driveId+" -> "+data);
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
        if(!gapi.isConnected())
            return; //Stop the presses
        ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
                new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(TAG, "Error while opening the file contents");
                            return;
                        }
                        Log.d(TAG, "File contents opened");

                        //Now step 2, build the contents
                        try {
                            DriveContents contents = result.getDriveContents();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(contents.getInputStream()));
                            StringBuilder builder = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                builder.append(line);
                            }
                            String contentsAsString = builder.toString();
                            Log.d(TAG, "From "+contents.getDriveId());
                            Log.d(TAG, "Retrieved "+contentsAsString);
                            //Step 3, write to SM
                            setString(resId, contentsAsString);

                            //Step 4, close stream (or not? java.lang.IllegalStateException: Cannot commit contents opened with MODE_READ_ONLY)
                            /*contents.commit(gapi, null).setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(Status result) {
                                    // handle the response status
                                }
                            });*/

                            //Step 5, send alert
                            if(gdl != null)
                                gdl.onActionFinished(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };

        Drive.DriveApi.getFile(gapi, driveId)
                .open(gapi, DriveFile.MODE_READ_ONLY, null)
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
                        gapi, DriveFile.MODE_WRITE_ONLY, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    return false;
                }
                driveContents = driveContentsResult.getDriveContents();
                OutputStream outputStream = driveContents.getOutputStream();
                outputStream.write(data.getBytes());
                com.google.android.gms.common.api.Status status =
                        driveContents.commit(gapi, null).await();
                if(gdl != null)
                    gdl.onActionFinished(false);
                return status.getStatus().isSuccess();
            } catch (IOException e) {
                Log.e(TAG, "IOException while appending to the output stream", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "EditContents result "+result);
            /*driveContents.commit(gapi, null).setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
                @Override
                public void onResult(com.google.android.gms.common.api.Status result) {
                    // Handle the response status
                }
            });*/

        }
    }
    public interface GoogleDriveListener {
        public abstract void onActionFinished(boolean cloudToLocal);
    }
}