package com.felkertech.n.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.util.Log;

import com.felkertech.channelsurfer.sync.SyncUtils;
import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.cumulustv.plugins.CumulusTvPlugin;
import com.felkertech.n.ActivityUtils;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JsonChannel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import org.json.JSONException;
import org.json.JSONObject;

public class DataReceiver extends BroadcastReceiver
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = DataReceiver.class.getSimpleName();
    private static final boolean DEBUG = false;

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

        if (DEBUG) {
            Log.d(TAG, "Heard");
        }
        if(intent != null) {
            String action = intent.getStringExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION);
            String jsonString = "";
            if (intent.hasExtra(CumulusTvPlugin.INTENT_EXTRA_JSON)) {
                jsonString = intent.getStringExtra(CumulusTvPlugin.INTENT_EXTRA_JSON);
            }
            if (action.equals(CumulusTvPlugin.INTENT_EXTRA_ACTION_DATABASE_WRITE)) {
                Log.d(TAG, "Received write command");
                gapi.connect();
            } else if (action.equals(CumulusTvPlugin.INTENT_EXTRA_ACTION_WRITE)) {
                Log.d(TAG, "Received " + jsonString);
                DriveSettingsManager sm = new DriveSettingsManager(context);
                ChannelDatabase cdn = ChannelDatabase.getInstance(context);
                try {
                    JSONObject jo = new JSONObject(jsonString);
                    CumulusChannel jsonChannel;
                    CumulusChannel.Builder builder = new JsonChannel.Builder(jo).setPluginSource(
                            intent.getStringExtra(CumulusTvPlugin.INTENT_EXTRA_SOURCE));
                    if (intent.hasExtra(CumulusTvPlugin.INTENT_EXTRA_ORIGINAL_JSON)) {
                        // Clearly edited a stream
                        JsonChannel original = new JsonChannel.Builder(
                                new JSONObject(intent.getStringExtra(
                                        CumulusTvPlugin.INTENT_EXTRA_ORIGINAL_JSON)))
                                .build();
                        for(int i = 0; i < cdn.getJsonChannels().size(); i++) {
                            JsonChannel item = cdn.getJsonChannels().get(i);
                            if(original.equals(item) && DEBUG) {
                                Log.d(TAG, "Found a match");
                            }
                        }
                    }
                    jsonChannel = builder.build();
                    if (cdn.channelExists(jsonChannel)) {
                        //Channel exists, so let's update
                        cdn.update(jsonChannel);
                        if (DEBUG) {
                            Log.d(TAG, "Channel updated");
                        }
                    } else {
                        cdn.add(jsonChannel);
                        if (DEBUG) {
                            Log.d(TAG, "Channel added");
                        }
                    }
                    gapi.connect();
                } catch (JSONException e) {
                    if (DEBUG) {
                        Log.e(TAG, e.getMessage() + "; Error while adding");
                    }
                    e.printStackTrace();
                }
            } else if (action.equals(CumulusTvPlugin.INTENT_EXTRA_ACTION_DELETE)) {
                ChannelDatabase cdn = ChannelDatabase.getInstance(context);
                try {
                    JSONObject jo = new JSONObject(jsonString);
                    JsonChannel jsonChannel = new JsonChannel.Builder(jo).build();
                    cdn.delete(jsonChannel);
                    if (DEBUG) {
                        Log.d(TAG, "Channel successfully deleted");
                    }
                    // Now sync
                    gapi.connect();
                } catch (JSONException e) {
                    if (DEBUG) {
                        Log.e(TAG, e.getMessage() + "; Error while adding");
                    }
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (DEBUG) {
            Log.d(TAG, "Connected - Sync w/ drive");
        }
        //Let's sync with GDrive
        ActivityUtils.writeDriveData(mContext, gapi);

        final String info = TvContract.buildInputId(ActivityUtils.TV_INPUT_SERVICE);
        SyncUtils.requestSync(mContext, info);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
}
