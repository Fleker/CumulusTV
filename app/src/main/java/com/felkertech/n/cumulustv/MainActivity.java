package com.felkertech.n.cumulustv;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.tv.TvContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.android.sampletvinput.syncadapter.SyncUtils;
import com.felkertech.n.boilerplate.Utils.SettingsManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import org.json.JSONException;
import org.w3c.dom.Text;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static String TAG = "cumulus:MainActivity";
    GoogleApiClient gapi;
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_CREATOR = 102;
    private static final int REQUEST_CODE_OPENER = 104;
    SettingsManager sm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");
        final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
        gapi = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        sm = new SettingsManager(this);
        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title("Create a new channel")
                        .customView(R.layout.dialog_channel_new, true)
                        .positiveText("Create")
                        .negativeText("Cancel")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                Log.d(TAG, "Submission");
                                //Get stuff
                                LinearLayout l = (LinearLayout) dialog.getCustomView();
                                String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                Log.d(TAG, "Channel " + number);
                                String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                String splash = ((EditText) l.findViewById(R.id.splash)).getText().toString();
                                ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                try {
                                    Log.d(TAG, cd.toString());
                                    JSONChannel jsch = new JSONChannel(number, name, stream, logo, splash, "");
                                    if (cd.channelExists(jsch)) {
                                        Toast.makeText(getApplicationContext(), "Channel already exists", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "no");
                                    } else {
                                        cd.add(jsch);
                                        Log.d(TAG, "K");
                                    }
                                    Log.d(TAG, cd.toString());
                                    writeDriveData();
//                                    SyncUtils.requestSync(info);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .show();

            }
        });
        findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
                String[] channelnames = cdn.getChannelNames();
                if(channelnames.length == 0) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title("You have no streams")
                            .content("Find a few streams to add")
                            .positiveText("OK")
                            .negativeText("No")
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
                            .title("My Streams")
                            .items(channelnames)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, final int i, CharSequence charSequence) {
                                    Toast.makeText(getApplicationContext(), charSequence + " selected", Toast.LENGTH_SHORT).show();
                                    final MaterialDialog md = new MaterialDialog.Builder(MainActivity.this)
                                            .title("Edit Stream")
                                            .positiveText("Update")
                                            .negativeText("Delete")
                                            .neutralText("Cancel")
                                            .customView(R.layout.dialog_channel_new, true)
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    super.onPositive(dialog);
                                                    LinearLayout l = (LinearLayout) dialog.getCustomView();
                                                    String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                                    Log.d(TAG, "Channel " + number);
                                                    String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                                    String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                                    String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                                    String splash = ((EditText) l.findViewById(R.id.splash)).getText().toString();
                                                    ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                                    try {
                                                        Log.d(TAG, cd.toString());
                                                        JSONChannel jsch = new JSONChannel(number, name, stream, logo, splash, "");
                                                        cd.update(jsch, i);
                                                        Log.d(TAG, cd.toString());
                                                        writeDriveData();
//                                                        SyncUtils.requestSync(info);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }

                                                @Override
                                                public void onNegative(MaterialDialog dialog) {
                                                    super.onNegative(dialog);
                                                    final LinearLayout l = (LinearLayout) dialog.getCustomView();
                                                    String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                                    Log.d(TAG, "Channel " + number);
                                                    new MaterialDialog.Builder(MainActivity.this)
                                                            .title("Delete?")
                                                            .positiveText("Yes")
                                                            .negativeText("No")
                                                            .callback(new MaterialDialog.ButtonCallback() {
                                                                @Override
                                                                public void onPositive(MaterialDialog dialog) {
                                                                    super.onPositive(dialog);
                                                                    String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                                                    Log.d(TAG, "DEL Channel " + number);
                                                                    String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                                                    String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                                                    String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                                                    String splash = ((EditText) l.findViewById(R.id.splash)).getText().toString();
                                                                    ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                                                    try {
                                                                        Log.d(TAG, cd.toString());
                                                                        JSONChannel jsch = new JSONChannel(number, name, stream, logo, splash, "");
                                                                        cd.delete(jsch);
                                                                        Log.d(TAG, cd.toString());
                                                                        writeDriveData();
//                                                                        SyncUtils.requestSync(info);
                                                                    } catch (JSONException e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                }
                                                            }).show();
                                                }
                                            })
                                            .show();
                                    md.setOnShowListener(new DialogInterface.OnShowListener() {
                                        @Override
                                        public void onShow(DialogInterface dialog) {
                                            try {
                                                JSONChannel jsonChannel = new JSONChannel(cdn.getJSONChannels().getJSONObject(i));
                                                LinearLayout l = (LinearLayout) md.getCustomView();
                                                ((EditText) l.findViewById(R.id.number)).setText(jsonChannel.getNumber());
                                                Log.d(TAG, "Channel " + jsonChannel.getNumber());
                                                ((EditText) l.findViewById(R.id.name)).setText(jsonChannel.getName());
                                                ((EditText) l.findViewById(R.id.logo)).setText(jsonChannel.getLogo());
                                                ((EditText) l.findViewById(R.id.stream)).setText(jsonChannel.getUrl());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            })
                            .show();
                }
            }
        });
        findViewById(R.id.suggested).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                0xx NEWS
                1xx SCIENCE/TECH/NATURE
                2xx HISTORY/EDUCATION
                3xx SPORTS/VIDEO GAMES
                4xx MUSIC
                5xx FICTION
                6xx NONFICTION
                7xx GOVERNMENT/SOCIETY
                9xx MISC
                 */
                final JSONChannel[] channels = { /* Some via http://rgw.ustream.tv/json.php/Ustream.searchBroadcast/ */
                        new JSONChannel("900",
                                "Artbeats Demo",
                                "http://cdn-fms.rbs.com.br/hls-vod/sample1_1500kbps.f4v.m3u8", "", "",
                                TvContract.Programs.Genres.ARTS),
                        new JSONChannel("100",
                                "NASA Public",
                                "http://iphone-streaming.ustream.tv/uhls/6540154/streams/live/iphone/playlist.m3u8",
                                "http://static-cdn1.ustream.tv/i/channel/live/1_6540154,256x144,b:2015071514.jpg", "",
                                TvContract.Programs.Genres.TECH_SCIENCE),
                        new JSONChannel("167",
                                "Montery Bay Aquarium",
                                "http://iphone-streaming.ustream.tv/uhls/9600798/streams/live/iphone/playlist.m3u8",
                                "http://static-cdn1.ustream.tv/i/channel/live/1_9600798,256x144,b:2015071514.jpg", "",
                                TvContract.Programs.Genres.ANIMAL_WILDLIFE),
                        new JSONChannel("168",
                                "Audubon Osprey Cam",
                                "http://iphone-streaming.ustream.tv/uhls/11378037/streams/live/iphone/playlist.m3u8",
                                "http://static-cdn1.ustream.tv/i/channel/live/1_11378037,256x144,b:2015071514.jpg", "",
                                TvContract.Programs.Genres.ANIMAL_WILDLIFE),
                        new JSONChannel("101",
                                "ISS Stream",
                                "http://iphone-streaming.ustream.tv/uhls/9408562/streams/live/iphone/playlist.m3u8",
                                "http://static-cdn1.ustream.tv/i/channel/picture/9/4/0/8/9408562/9408562_iss_hr_1330361780,256x144,r:1.jpg", "",
                                TvContract.Programs.Genres.TECH_SCIENCE),
//                        new JSONChannel("400", "Beats One", "http://stream.connectcast.tv:1935/live/CC-EC1245DB-5C6A-CF57-D13A-BB36B3CBB488-34313/playlist.m3u8", "")
                        new JSONChannel("401",
                                "OutOfFocus.TV",
                                "http://pablogott.videocdn.scaleengine.net/pablogott-iphone/play/ooftv1/playlist.m3u8",
                                "http://i.imgur.com/QRCIhN4.png", "",
                                TvContract.Programs.Genres.MUSIC),
/*
                        new JSONChannel("900", "Euronews De", "http://fr-par-iphone-2.cdn.hexaglobe.net/streaming/euronews_ewns/14-live.m3u8", ""),
                        new JSONChannel("901", "TVI (Portugal)", "http://noscdn1.connectedviews.com:1935/live/smil:tvi.smil/playlist.m3u8", ""),
                        new JSONChannel("902", "PHOENIXHD", "http://teleboy.customers.cdn.iptv.ch/1122/index.m3u8", ""),
                        new JSONChannel("903", "Sport 1 Germany", "http://streaming-hub.com/tv/i/sport1_1@97464/index_1300_av-p.m3u8?sd=10&rebase=on", ""),
                        new JSONChannel("904", "RTP International", "http://rtp-pull-live.hls.adaptive.level3.net/liverepeater/rtpi_5ch120h264.stream/livestream.m3u8", "")
*/
                };
                ArrayList<String> channeltext = new ArrayList<String>();
                for(JSONChannel j: channels) {
                    channeltext.add(j.getName());
                }
                final String[] channelList = channeltext.toArray(new String[channeltext.size()]);
                new MaterialDialog.Builder(MainActivity.this)
                        .title("Here are some suggested streams:")
                        .items(channelList)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                JSONChannel j = channels[i];
                                ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                if(cd.channelExists(j)) {
                                    Toast.makeText(getApplicationContext(), "Channel already added", Toast.LENGTH_SHORT).show();
                                } else {
                                    try {
                                        Toast.makeText(getApplicationContext(), charSequence+" has been added", Toast.LENGTH_SHORT).show();
                                        cd.add(j);
                                        writeDriveData();
//                                        SyncUtils.requestSync(info);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).show();
            }
        });
        findViewById(R.id.gotoapp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = getPackageManager().getLaunchIntentForPackage("com.google.android.tv");
                startActivity(i);
            }
        });
        /*findViewById(R.id.osl).setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
               new MaterialDialog.Builder(MainActivity.this)
                       .title("Open Source Licenses")
                       .content(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(getApplicationContext()))
                       .show();
           }
        });*/
        findViewById(R.id.gdrive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gapi.connect();
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
        sm.setGoogleDriveSyncable(gapi, new SettingsManager.GoogleDriveListener() {
            @Override
            public void onActionFinished() {
                Log.d(TAG, "Sync req after drive action");
                final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
                SyncUtils.requestSync(info);
            }
        }); //Enable GDrive
        Log.d(TAG, sm.getString(R.string.sm_google_drive_id)+"<< for onConnected");
        if(sm.getString(R.string.sm_google_drive_id).isEmpty()) {
            //We need a new file
            new MaterialDialog.Builder(MainActivity.this)
                    .title("Create a syncable file")
                    .content("Save channel info in Google Drive so you can always access it")
                    .positiveText("OK")
                    .negativeText("No")
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
            readDriveData();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!new SettingsManager(this).getString(R.string.sm_google_drive_id).isEmpty()) {
            gapi.connect();
            findViewById(R.id.gdrive).setVisibility(View.GONE);
            findViewById(R.id.gdrive).setEnabled(false);
        }
        Log.d(TAG, "onStart");
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.version)).setText("Version "+pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "oCF "+connectionResult.toString());
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "App connect +1");
                    gapi.connect();
                } else {
                    Log.d(TAG, "App cannot connect");
                    new MaterialDialog.Builder(this)
                            .title("Connection Issue")
                            .content("Cannot connect to Google Drive at this moment.")
                            .positiveText("OK")
                            .negativeText("Try Again")
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onNegative(MaterialDialog dialog) {
                                    super.onNegative(dialog);
                                    gapi.connect();
                                }
                            }).show();
                }
                break;
            case REQUEST_CODE_CREATOR:
                if(data == null) //If op was canceled
                    return;
                DriveId driveId = (DriveId) data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                Log.d(TAG, driveId.encodeToString()+", "+driveId.getResourceId()+", "+driveId.toInvariantString());
                sm.setString(R.string.sm_google_drive_id, driveId.encodeToString());

                DriveFile file = Drive.DriveApi.getFile(gapi,DriveId.decodeFromString(driveId.encodeToString()));
                //Write initial data
                writeDriveData();
                break;
            case REQUEST_CODE_OPENER:
                if(data == null) //If op was canceled
                    return;
                driveId = (DriveId) data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                Log.d(TAG, driveId.encodeToString()+", "+driveId.getResourceId()+", "+driveId.toInvariantString());
                sm.setString(R.string.sm_google_drive_id, driveId.encodeToString());
                new MaterialDialog.Builder(this)
                        .title("Choose Initial Action")
                        .positiveText("Write cloud data to local")
                        .negativeText("Write local data to cloud")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                readDriveData();
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                super.onNegative(dialog);
                                writeDriveData();
                            }
                        })
                        .show();
        }
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
    public void oslClick() {
        new MaterialDialog.Builder(MainActivity.this)
                .title("Software Licenses")
                .content(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(getApplicationContext()))
                .show();
    }
    public void moreClick() {
        String[] actions = new String[] {"Switch Google Drive file", "Refresh Data - Cloud to Local", "View Software Licenses", "Reset Channel Data"};
        new MaterialDialog.Builder(MainActivity.this)
                .title("More Actions")
                .items(actions)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        switch (i) {
                            case 0:
//                                Toast.makeText(MainActivity.this, "Not supported", Toast.LENGTH_SHORT).show();
                                IntentSender intentSender = Drive.DriveApi
                                        .newOpenFileActivityBuilder()
                                        .setMimeType(new String[]{ "application/json", "text/*" })
                                        .build(gapi);
                                try {
                                    startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.w(TAG, "Unable to send intent", e);
                                }
                                break;
                            case 1:
                                readDriveData();
                                break;
                            case 2:
                                oslClick();
                                break;
                            case 3:
                                new MaterialDialog.Builder(MainActivity.this)
                                        .title("Delete all your channel data?")
                                        .positiveText("Yes")
                                        .negativeText("NO")
                                        .callback(new MaterialDialog.ButtonCallback() {
                                            @Override
                                            public void onPositive(MaterialDialog dialog) {
                                                super.onPositive(dialog);
                                                sm.setString(ChannelDatabase.KEY, "{'channels':[]}");
                                                sm.writeToGoogleDrive(DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id)),
                                                        sm.getString(ChannelDatabase.KEY));
                                                sm.setString(R.string.sm_google_drive_id, "");
                                                Toast.makeText(MainActivity.this, "The deed was done", Toast.LENGTH_SHORT).show();
                                                onCreate(null);
                                            }
                                        })
                                        .show();
                                break;
                        }
                    }
                })
                .show();
    }
    public void readDriveData() {
        sm.readFromGoogleDrive(DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id)), ChannelDatabase.KEY);

        final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
        SyncUtils.requestSync(info);
    }
    public void writeDriveData() {
        sm.writeToGoogleDrive(DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id)), new ChannelDatabase(this).toString());

        final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
        SyncUtils.requestSync(info);
    }
}
