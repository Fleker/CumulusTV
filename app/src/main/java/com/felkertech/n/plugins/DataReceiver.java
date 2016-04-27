package com.felkertech.n.plugins;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.util.Log;

import com.felkertech.channelsurfer.sync.SyncUtils;
import com.felkertech.n.ActivityUtils;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JSONChannel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import org.json.JSONException;
import org.json.JSONObject;

public class DataReceiver extends BroadcastReceiver
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static String INTENT_EXTRA_JSON = "JSON";
    public static String INTENT_EXTRA_ORIGINAL_JSON = "OGJSON";
    public static String INTENT_EXTRA_ACTION = "Dowhat";
    public static String INTENT_EXTRA_ACTION_WRITE = "Write";
    public static String INTENT_EXTRA_ACTION_DELETE = "Delete";
    public static String INTENT_EXTRA_ACTION_DATABASE_WRITE = "SaveDatabase";
    public static String INTENT_EXTRA_SOURCE = "Source";
    public static String TAG = "cumulus:DataReceiver";

    private GoogleApiClient gapi;
    private Context mContext;
    public DataReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        gapi = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Log.d(TAG, "Heard");
        if(intent != null) {
            String action = intent.getStringExtra(INTENT_EXTRA_ACTION);
            String jsonString = "";
            if(intent.hasExtra(INTENT_EXTRA_JSON))
                jsonString = intent.getStringExtra(INTENT_EXTRA_JSON);

            if(action.equals(INTENT_EXTRA_ACTION_DATABASE_WRITE)) {
                Log.d(TAG, "Received write command");
                gapi.connect();
            } else if(action.equals(INTENT_EXTRA_ACTION_WRITE)) {
                Log.d(TAG, "Received " + jsonString);
                DriveSettingsManager sm = new DriveSettingsManager(context);
                ChannelDatabase cdn = new ChannelDatabase(context);
                try {
                    JSONObject jo = new JSONObject(jsonString);
                    JSONChannel jsonChannel = new JSONChannel(jo);
                    jsonChannel.setSource(intent.getStringExtra(INTENT_EXTRA_SOURCE));
                    if(intent.hasExtra(INTENT_EXTRA_ORIGINAL_JSON)) {
                        //Clearly edited a stream
                        JSONChannel original = new JSONChannel(new JSONObject(intent.getStringExtra(INTENT_EXTRA_ORIGINAL_JSON)));
                        for(int i = 0; i < cdn.getJSONChannels().length(); i++) {
                            JSONChannel item = new JSONChannel(cdn.getJSONChannels().getJSONObject(i));
                            if(original.equals(item)) {
                                Log.d(TAG, "Found a match");

                            }
                        }
                    } else {

                    }
                    if (cdn.channelExists(jsonChannel)) {
                        //Channel exists, so let's update
                        cdn.update(jsonChannel);
                        Log.d(TAG, "Channel updated");
                    } else {
                        cdn.add(jsonChannel);
                        Log.d(TAG, "Channel added");
                    }
                    gapi.connect();
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage() + "; Error while adding");
                    e.printStackTrace();
                }
            } else if(action.equals(INTENT_EXTRA_ACTION_DELETE)) {
                ChannelDatabase cdn = new ChannelDatabase(context);
                try {
                    JSONObject jo = new JSONObject(jsonString);
                    JSONChannel jsonChannel = new JSONChannel(jo);
                    cdn.delete(jsonChannel);
                    Log.d(TAG, "Channel successfully deleted");
                    //Now sync
                    gapi.connect();
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage() + "; Error while adding");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected. Now what?");
        //Let's sync with GDrive
        ActivityUtils.writeDriveData(mContext, gapi);
        Log.d(TAG, "Sync w/ drive");

        final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".CumulusTvService"));
        SyncUtils.requestSync(mContext, info);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
