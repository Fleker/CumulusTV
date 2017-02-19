package com.felkertech.cumulustv.activities;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.felkertech.cumulustv.fileio.CloudStorageProvider;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.services.CumulusJobService;
import com.felkertech.cumulustv.tv.activities.LeanbackActivity;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.cumulustv.utils.AppUtils;
import com.felkertech.cumulustv.utils.DriveSettingsManager;
import com.felkertech.cumulustv.widgets.ChannelShortcut;
import com.felkertech.n.cumulustv.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.MetadataChangeSet;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.fabric.sdk.android.Fabric;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_CREATOR = 102;

    private DriveSettingsManager sm;
    private AlertDialog md;

    private AlertDialog.OnClickListener genreSelectionCallback = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {

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
        CloudStorageProvider.getInstance().autoConnect(this);
        try {
            final ChannelDatabase channelDatabase = ChannelDatabase.getInstance(MainActivity.this);
            Fabric.with(this, new Crashlytics());
            if(AppUtils.isTV(this)) {
                // Go to tv activity
                Intent leanbackIntent = new Intent(this, LeanbackActivity.class);
                leanbackIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                startActivity(leanbackIntent);
            }
            updateUi();
        } catch (ChannelDatabase.MalformedChannelDataException e) {
            ActivityUtils.handleMalformedChannelData(MainActivity.this,
                    CloudStorageProvider.getInstance().getClient(), e);
        }
    }

    public void updateUi() {
        final ChannelDatabase channelDatabase = ChannelDatabase.getInstance(MainActivity.this);

        findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] channelnames = channelDatabase.getChannelNames();
                if(channelnames.length == 0) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.no_channels)
                            .setMessage(R.string.no_channels_find)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                    ActivityUtils.openSuggestedChannels(MainActivity.this,
                                            CloudStorageProvider.getInstance().getClient());
                                }
                            })
                            .setNegativeButton(R.string.no, null)
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
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.select_genres)
                            .setItems(genreArray, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int position) {
                                    // Now only get certain channels
                                    String selectedGenre = genreArray[position];
                                    List<JsonChannel> jsonChannelList = new ArrayList<>();
                                    List<String> channelNames = new ArrayList<>();
                                    try {
                                        for (JsonChannel jsonChannel :
                                                channelDatabase.getJsonChannels()) {
                                            if (jsonChannel.getGenresString() != null
                                                    && jsonChannel.getGenresString()
                                                    .contains(selectedGenre)) {
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
        findViewById(R.id.gdrive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CloudStorageProvider.getInstance().connect(MainActivity.this);
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
        ActivityUtils.onConnected(CloudStorageProvider.getInstance().getClient());
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
        sm.setGoogleDriveSyncable(CloudStorageProvider.getInstance().getClient(), new DriveSettingsManager.GoogleDriveListener() {
            @Override
            public void onActionFinished(boolean cloudToLocal) {
                Log.d(TAG, "Sync req after drive action");
                final String info = TvContract.buildInputId(ActivityUtils.TV_INPUT_SERVICE);
                CumulusJobService.requestImmediateSync1(MainActivity.this, info, CumulusJobService.DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS,
                        new ComponentName(MainActivity.this, CumulusJobService.class));
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
            // We need a new file
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.create_syncable_file)
                    .setMessage(R.string.create_syncable_file_description)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Drive.DriveApi.newDriveContents(ActivityUtils.getGoogleApiClient())
                                    .setResultCallback(driveContentsCallback);
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            //Great, user already has sync enabled, let's resync
            ActivityUtils.readDriveData(MainActivity.this, ActivityUtils.getGoogleApiClient());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        CloudStorageProvider.getInstance().autoConnect(this);
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
                Toast.makeText(this, "Error connecting: " + connectionResult.toString(),
                        Toast.LENGTH_SHORT).show();
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
        ActivityUtils.onActivityResult(MainActivity.this,
                CloudStorageProvider.getInstance().getClient(), requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ActivityUtils.PERMISSION_EXPORT_M3U:
                if (grantResults[0] == PERMISSION_GRANTED) {
                    ActivityUtils.exportM3uPlaylist(MainActivity.this);
                }
                break;
        }
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
                            .build(CloudStorageProvider.getInstance().getClient());
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
                getString(R.string.export_m3u),
                getString(R.string.settings_refresh_cloud_local),
            };
        new AlertDialog.Builder(this)
                .setTitle(R.string.more_actions)
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                ActivityUtils.browsePlugins(MainActivity.this);
                                break;
                            case 1: // Export data as M3u
                                if (Build.VERSION.SDK_INT >= 23) {
                                    // Check if we're allowed to do this
                                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                            == PERMISSION_DENIED) {
                                        requestPermissions(new String[]
                                                        {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                ActivityUtils.PERMISSION_EXPORT_M3U);
                                        break;
                                    }
                                }
                                ActivityUtils.exportM3uPlaylist(MainActivity.this);
                                break;
                            case 2:
                                ActivityUtils.readDriveData(MainActivity.this,
                                        CloudStorageProvider.getInstance().getClient());
                                break;
                            case 7:
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(R.string.about_mlc)
                                        .setMessage(R.string.about_mlc_summary)
                                        .setPositiveButton(R.string.about_mlc_issues, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent gi = new Intent(Intent.ACTION_VIEW);
                                                gi.setData(Uri.parse("https://bitbucket.org/fleke" +
                                                        "r/mlc-music-live-channels"));
                                                startActivity(gi);
                                            }
                                        })
                                        .show();
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
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(label)
                .setItems(channelNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        JsonChannel jsonChannel = jsonChannels.get(i);
                        ActivityUtils.editChannel(MainActivity.this, jsonChannel.getMediaUrl());
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_mobile, menu);
        getSupportActionBar().show();
        getSupportActionBar().setBackgroundDrawable(
                new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                ActivityUtils.openPluginPicker(true, MainActivity.this);
                break;
            case R.id.menu_add_suggested:
                ActivityUtils.openSuggestedChannels(MainActivity.this,
                        CloudStorageProvider.getInstance().getClient());
                break;
            case R.id.menu_import:
                ActivityUtils.switchGoogleDrive(MainActivity.this,
                        CloudStorageProvider.getInstance().getClient());
                break;
            case R.id.menu_licenses:
                ActivityUtils.oslClick(MainActivity.this);
                break;
            case R.id.menu_reset_data:
                ActivityUtils.deleteChannelData(MainActivity.this,
                        CloudStorageProvider.getInstance().getClient());
                break;
            case R.id.menu_about:
                ActivityUtils.openAbout(MainActivity.this);
                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
