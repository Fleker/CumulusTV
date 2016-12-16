package com.felkertech.cumulustv.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.felkertech.channelsurfer.sync.SyncUtils;
import com.felkertech.cumulustv.fileio.CloudStorageProvider;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.cumulustv.utils.AppUtils;
import com.felkertech.cumulustv.utils.DriveSettingsManager;
import com.felkertech.n.cumulustv.R;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.widgets.ChannelShortcut;
import com.felkertech.cumulustv.fileio.XmlTvParser;
import com.felkertech.cumulustv.tv.activities.LeanbackActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.MetadataChangeSet;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_CREATOR = 102;

    private GoogleApiClient gapi;
    private DriveSettingsManager sm;
    private MaterialDialog md;

    private MaterialDialog.ListCallback genreSelectionCallback = new MaterialDialog.ListCallback() {
        @Override
        public void onSelection(MaterialDialog dialog, View itemView,
                int position, CharSequence text) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        ChannelShortcut.updateWidgets(this, ChannelShortcut.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");
        sm = new DriveSettingsManager(this);
        ActivityUtils.openIntroIfNeeded(this);
        final ChannelDatabase channelDatabase = ChannelDatabase.getInstance(MainActivity.this);
        Fabric.with(this, new Crashlytics());
        if(AppUtils.isTV(this)) {
            // Go to tv activity
            Intent leanbackIntent = new Intent(this, LeanbackActivity.class);
            leanbackIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(leanbackIntent);
        }

        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityUtils.openPluginPicker(true, MainActivity.this);
            }
        });
        findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] channelnames = channelDatabase.getChannelNames();
                if(channelnames.length == 0) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title(R.string.no_channels)
                            .content(R.string.no_channels_find)
                            .positiveText(R.string.ok)
                            .negativeText(R.string.no)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);
                                    dialog.cancel();
                                    findViewById(R.id.suggested).performClick();
                                }
                            })
                            .show();
                } else {
                    try {
                        displayChannelPicker(channelDatabase.getJsonChannels(), channelnames);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        findViewById(R.id.view_genres).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Generate genres
                Set<String> genreSet = new HashSet<>();
                try {
                    for (JsonChannel jsonChannel : channelDatabase.getJsonChannels()) {
                        Collections.addAll(genreSet, jsonChannel.getGenres());
                    }
                    final String[] genreArray = genreSet.toArray(new String[genreSet.size()]);
                    new MaterialDialog.Builder(MainActivity.this)
                            .title(R.string.select_genres)
                            .items(genreArray)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View itemView,
                                        int position, CharSequence text) {
                                    // Now only get certain channels
                                    String selectedGenre = genreArray[position];
                                    List<JsonChannel> jsonChannelList = new ArrayList<>();
                                    List<String> channelNames = new ArrayList<>();
                                    try {
                                        for (JsonChannel jsonChannel :
                                                channelDatabase.getJsonChannels()) {
                                            if (jsonChannel.getGenresString().contains(selectedGenre)) {
                                                jsonChannelList.add(jsonChannel);
                                                channelNames.add(jsonChannel.getNumber() + " " +
                                                    jsonChannel.getName());
                                            }
                                        }
                                        displayChannelPicker(jsonChannelList, channelNames
                                                .toArray(new String[channelNames.size()]),
                                                selectedGenre);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.suggested).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityUtils.openSuggestedChannels(MainActivity.this, gapi);
            }
        });
        findViewById(R.id.gdrive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gapi = CloudStorageProvider.getInstance().connect(MainActivity.this);
            }
        });
        findViewById(R.id.more_actions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreClick();
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        if(md != null) {
            if(md.isShowing()) {
                try {
                    md.dismiss();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        CloudStorageProvider.getInstance().onConnected(Activity.RESULT_OK);
        sm.setGoogleDriveSyncable(gapi, new DriveSettingsManager.GoogleDriveListener() {
            @Override
            public void onActionFinished(boolean cloudToLocal) {
                Log.d(TAG, "Sync req after drive action");
                final String info = TvContract.buildInputId(ActivityUtils.TV_INPUT_SERVICE);
                SyncUtils.requestSync(MainActivity.this, info);
                if (cloudToLocal) {
                    Toast.makeText(MainActivity.this, R.string.downloaded, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.uploaded, Toast.LENGTH_SHORT).show();
                }
            }
        }); //Enable GDrive
        if (DEBUG) {
            Log.d(TAG, sm.getString(R.string.sm_google_drive_id) + "<< for onConnected");
        }
        if(sm.getString(R.string.sm_google_drive_id).isEmpty()) {
            //We need a new file
            new MaterialDialog.Builder(MainActivity.this)
                    .title(R.string.create_syncable_file)
                    .content(R.string.create_syncable_file_description)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.no)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            Drive.DriveApi.newDriveContents(gapi)
                                    .setResultCallback(driveContentsCallback);
                        }
                    })
                    .show();
        } else {
            //Great, user already has sync enabled, let's resync
            ActivityUtils.readDriveData(MainActivity.this, gapi);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        /*if(!new DriveSettingsManager(this).getString(R.string.sm_google_drive_id).isEmpty()) {
            if(!gapi.isConnected()) {
                md = new MaterialDialog.Builder(this)
                        .customView(R.layout.load_dialog, false)
                        .show();
                findViewById(R.id.gdrive).setVisibility(View.GONE);
                findViewById(R.id.gdrive).setEnabled(false);
                Log.d(TAG, "Need to connect");
            } else {
                if(md != null) {
                    if(md.isShowing())
                        md.dismiss();
                }
            }

        } else {
        }*/
        CloudStorageProvider.getInstance().autoConnect(this);
        /*md = new MaterialDialog.Builder(this)
                .customView(R.layout.load_dialog, false)
                .show();*/
        Log.d(TAG, "onStart");
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.version)).setText(getString(R.string.version, pInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (DEBUG) {
            Log.d(TAG, "Error connecting " + connectionResult.getErrorCode());
        }
        if(md != null) {
            if (md.isShowing()) {
                ((TextView) md.getCustomView().findViewById(R.id.message))
                        .setText("Error connecting: " + connectionResult.toString());
            }
        }
        Toast.makeText(MainActivity.this, "Connection issue (" + connectionResult.getErrorCode() +
                "): " + connectionResult.toString(), Toast.LENGTH_SHORT).show();
        if (DEBUG) {
            Log.d(TAG, "oCF " + connectionResult.toString());
        }
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                Toast.makeText(MainActivity.this, "Cannot resolve issue", Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(),
                        MainActivity.this, 0).show();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        ActivityUtils.onActivityResult(MainActivity.this, gapi, requestCode, resultCode, data);
    }

    /** GDRIVE **/
    ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                            .setTitle("cumulustv_channels.json")
                            .setDescription("JSON list of channels that can be imported using " +
                                    "CumulusTV to view live streams")
                            .setMimeType("application/json").build();
                    IntentSender intentSender = Drive.DriveApi
                            .newCreateFileActivityBuilder()
                            .setActivityTitle("cumulustv_channels.json")
                            .setInitialMetadata(metadataChangeSet)
                            .setInitialDriveContents(result.getDriveContents())
                            .build(gapi);
                    try {
                        startIntentSenderForResult(
                                intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.w(TAG, "Unable to send intent", e);
                    }
                }
            };
    public void moreClick() {
        String[] actions = new String[] {
                getString(R.string.settings_browse_plugins),
                getString(R.string.settings_switch_google_drive),
                getString(R.string.settings_refresh_cloud_local),
                getString(R.string.settings_view_licenses),
                getString(R.string.settings_reset_channel_data),
                getString(R.string.settings_about)/*,
                getString(R.string.about_mlc)*/
            };
        new MaterialDialog.Builder(this)
                .title(R.string.more_actions)
                .items(actions)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        switch (i) {
                            case 0:
                                ActivityUtils.browsePlugins(MainActivity.this);
                                break;
                            case 1:
                                ActivityUtils.switchGoogleDrive(MainActivity.this, gapi);
                                break;
                            case 2:
                                ActivityUtils.readDriveData(MainActivity.this, gapi);
                                break;
                            case 3:
                                ActivityUtils.oslClick(MainActivity.this);
                                break;
                            case 4:
                                ActivityUtils.deleteChannelData(MainActivity.this, gapi);
                                break;
                            case 5:
                                ActivityUtils.openAbout(MainActivity.this);
                                break;
                            case 6:
                                new MaterialDialog.Builder(MainActivity.this)
                                        .title(R.string.about_mlc)
                                        .content(R.string.about_mlc_summary)
                                        .positiveText(R.string.about_mlc_issues)
                                        .callback(new MaterialDialog.ButtonCallback() {
                                            @Override
                                            public void onPositive(MaterialDialog dialog) {
                                                super.onPositive(dialog);
                                                Intent gi = new Intent(Intent.ACTION_VIEW);
                                                gi.setData(Uri.parse("https://bitbucket.org/fleke" +
                                                        "r/mlc-music-live-channels"));
                                                startActivity(gi);
                                            }
                                        })
                                        .show();
                            case 13:
                                final OkHttpClient client = new OkHttpClient();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Request request = new Request.Builder()
                                                .url("http://felkerdigitalmedia.com/sampletv.xml")
                                                .build();

                                        Response response = null;
                                        try {
                                            response = client.newCall(request).execute();
//                                            Log.d(TAG, response.body().string().substring(0,36));
                                            String s = response.body().string();
                                            List<com.felkertech.channelsurfer.model.Program> programs = XmlTvParser.parse(response.body().byteStream()).getAllPrograms();
                                            /*Log.d(TAG, programs.toString());
                                            Log.d(TAG, "Parsed "+programs.size());
                                            Log.d(TAG, "Program 1: "+ programs.get(0).getTitle());*/
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                                break;
                        }
                    }
                })
                .show();
    }

    public void displayChannelPicker(final List<JsonChannel> jsonChannels, String[] channelNames) {
        displayChannelPicker(jsonChannels, channelNames, getString(R.string.my_channels));
    }

    public void displayChannelPicker(final List<JsonChannel> jsonChannels, String[] channelNames,
             String label) {
        new MaterialDialog.Builder(MainActivity.this)
                .title(label)
                .items(channelNames)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, final int i,
                            CharSequence charSequence) {
                        JsonChannel jsonChannel = jsonChannels.get(i);
                        ActivityUtils.editChannel(MainActivity.this, jsonChannel.getMediaUrl());
                    }
                })
                .show();
    }
}
