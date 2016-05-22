package com.felkertech.n.cumulustv.activities;

import android.content.ComponentName;
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
import com.felkertech.n.ActivityUtils;
import com.felkertech.n.boilerplate.Utils.AppUtils;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JSONChannel;
import com.felkertech.n.cumulustv.xmltv.Program;
import com.felkertech.n.cumulustv.xmltv.XMLTVParser;
import com.felkertech.n.tv.LeanbackActivity;
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
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static String TAG = "cumulus:MainActivity";
    GoogleApiClient gapi;
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_CREATOR = 102;
    DriveSettingsManager sm;
    MaterialDialog md;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");
        //final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".CumulusTvService"));
        sm = new DriveSettingsManager(this);
        ActivityUtils.openIntroIfNeeded(this);
        Fabric.with(this, new Crashlytics());

        if(!AppUtils.isTV(this)) {
            findViewById(R.id.gotoapp).setVisibility(View.GONE);
        } else {
            //Go to tv activity
            startActivity(new Intent(this, LeanbackActivity.class));
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
                final ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
                String[] channelnames = cdn.getChannelNames();
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
                    new MaterialDialog.Builder(MainActivity.this)
                            .title(R.string.my_channels)
                            .items(channelnames)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, final int i, CharSequence charSequence) {
                                    //Toast.makeText(getApplicationContext(), charSequence + " selected", Toast.LENGTH_SHORT).show();
                                    try {
                                        JSONChannel jsonChannel = new JSONChannel(cdn.getJSONChannels().getJSONObject(i));
                                        ActivityUtils.editChannel(MainActivity.this, jsonChannel.getNumber());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .show();
                }
            }
        });
        findViewById(R.id.suggested).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityUtils.openSuggestedChannels(MainActivity.this, gapi);
            }
        });
        findViewById(R.id.gotoapp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityUtils.launchLiveChannels(MainActivity.this);
            }
        });
        findViewById(R.id.gdrive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gapi = ActivityUtils.GoogleDrive.connect(MainActivity.this);
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
        sm.setGoogleDriveSyncable(gapi, new DriveSettingsManager.GoogleDriveListener() {
            @Override
            public void onActionFinished(boolean cloudToLocal) {
                Log.d(TAG, "Sync req after drive action");
                final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".CumulusTvService"));
                SyncUtils.requestSync(MainActivity.this, info);
                if (cloudToLocal) {
                    Toast.makeText(MainActivity.this, R.string.downloaded, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.uploaded, Toast.LENGTH_SHORT).show();
                }
            }
        }); //Enable GDrive
        Log.d(TAG, sm.getString(R.string.sm_google_drive_id)+"<< for onConnected");
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
        ActivityUtils.GoogleDrive.autoConnect(this);
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
        Log.d(TAG, "Error connecting " + connectionResult.getErrorCode());
        if(md != null) {
            if (md.isShowing()) {
                ((TextView) md.getCustomView().findViewById(R.id.message)).setText("Error connecting: " + connectionResult.toString());
            }
        }
        Toast.makeText(MainActivity.this, "Connection issue ("+connectionResult.getErrorCode()+"): "+connectionResult.toString(), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "oCF " + connectionResult.toString());
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                Toast.makeText(MainActivity.this, "Cannot resolve issue", Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), MainActivity.this, 0).show();
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
                            .setDescription("JSON list of channels that can be imported using CumulusTV to view live streams")
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
                                                gi.setData(Uri.parse("https://bitbucket.org/fleker/mlc-music-live-channels"));
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
                                            List<Program> programs = XMLTVParser.parse(s);
                                            /*Log.d(TAG, programs.toString());
                                            Log.d(TAG, "Parsed "+programs.size());
                                            Log.d(TAG, "Program 1: "+ programs.get(0).getTitle());*/
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (XmlPullParserException e) {
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
}
