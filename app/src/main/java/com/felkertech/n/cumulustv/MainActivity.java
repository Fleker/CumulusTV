package com.felkertech.n.cumulustv;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import com.example.android.sampletvinput.syncadapter.SyncUtils;
import com.felkertech.n.boilerplate.Utils.AppUtils;
import com.felkertech.n.boilerplate.Utils.SettingsManager;
import com.felkertech.n.cumulustv.Intro.Intro;
import com.felkertech.n.cumulustv.xmltv.Program;
import com.felkertech.n.cumulustv.xmltv.XMLTVParser;
import com.felkertech.n.plugins.CumulusTvPlugin;
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
import com.google.android.gms.*;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static String TAG = "cumulus:MainActivity";
    GoogleApiClient gapi;
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_CREATOR = 102;
    private static final int REQUEST_CODE_OPENER = 104;
    public static final int LAST_GOOD_BUILD = 18;
    SettingsManager sm;
    MaterialDialog md;

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
        if(sm.getInt(R.string.sm_last_version) < LAST_GOOD_BUILD) {
            startActivity(new Intent(this, Intro.class));
            finish();
            return;
        }
        Fabric.with(this, new Crashlytics());

        if(!AppUtils.isTV(this)) {
            findViewById(R.id.gotoapp).setVisibility(View.GONE);
        }
        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPluginPicker(true);
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
                                    try {
                                        JSONChannel jsonChannel = new JSONChannel(cdn.getJSONChannels().getJSONObject(i));
                                        if(jsonChannel.hasSource()) {
                                            //Search through all plugins for one of a given source
                                            PackageManager pm = getPackageManager();
                                            final String pack = jsonChannel.getSource().split(",")[0];
                                            boolean app_installed = false;
                                            try {
                                                pm.getPackageInfo(pack, PackageManager.GET_ACTIVITIES);
                                                app_installed = true;
                                                //Open up this particular activity
                                                Intent intent = new Intent();
                                                intent.setClassName(pack,
                                                        jsonChannel.getSource().split(",")[1]);
                                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
                                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NUMBER, jsonChannel.getNumber());
                                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NAME, jsonChannel.getName());
                                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_URL, jsonChannel.getUrl());
                                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ICON, jsonChannel.getLogo());
                                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_SPLASH, jsonChannel.getSplashscreen());
                                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_GENRES, jsonChannel.getGenresString());
                                                startActivity(intent);
                                            }
                                            catch (PackageManager.NameNotFoundException e) {
                                                app_installed = false;
                                                new MaterialDialog.Builder(MainActivity.this)
                                                        .title("Plugin "+pack+" not installed")
                                                        .content("What do you want to do instead?")
                                                        .positiveText("Download app")
                                                        .negativeText("Open in another plugin")
                                                        .callback(new MaterialDialog.ButtonCallback() {
                                                            @Override
                                                            public void onPositive(MaterialDialog dialog) {
                                                                super.onPositive(dialog);
                                                                Intent i = new Intent(android.content.Intent.ACTION_VIEW);
                                                                i.setData(Uri.parse("http://play.google.com/store/apps/details?id="+pack));
                                                                startActivity(i);
                                                            }

                                                            @Override
                                                            public void onNegative(MaterialDialog dialog) {
                                                                super.onNegative(dialog);
                                                                openPluginPicker(false, i);
                                                            }
                                                        }).show();
                                                Toast.makeText(MainActivity.this, "Plugin "+pack+" not installed.", Toast.LENGTH_SHORT).show();
                                                openPluginPicker(false, i);
                                            }
                                        } else {
                                            Log.d(TAG, "No specified source");
                                            openPluginPicker(false, i);
                                        }
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
                        new JSONChannel("100",
                                "NASA Public",
                                "http://iphone-streaming.ustream.tv/uhls/6540154/streams/live/iphone/playlist.m3u8",
                                "http://static-cdn1.ustream.tv/i/channel/live/1_6540154,256x144,b:2015071514.jpg", "",
                                TvContract.Programs.Genres.TECH_SCIENCE),
                        new JSONChannel("101",
                                "ISS Stream",
                                "http://iphone-streaming.ustream.tv/uhls/9408562/streams/live/iphone/playlist.m3u8",
                                "http://static-cdn1.ustream.tv/i/channel/picture/9/4/0/8/9408562/9408562_iss_hr_1330361780,256x144,r:1.jpg", "",
                                TvContract.Programs.Genres.TECH_SCIENCE),
                        new JSONChannel("133",
                                "TWiT (This Week in Tech)",
                                "http://twit.live-s.cdn.bitgravity.com/cdn-live-s1/_definst_/twit/live/high/playlist.m3u8",
                                "http://wiki.twit.tv/w/images/thumb/TWiT_Logo.svg.png/487px-TWiT_Logo.svg.png",
                                TvContract.Programs.Genres.TECH_SCIENCE, TvContract.Programs.Genres.NEWS),
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
//                        new JSONChannel("400", "Beats One", "http://stream.connectcast.tv:1935/live/CC-EC1245DB-5C6A-CF57-D13A-BB36B3CBB488-34313/playlist.m3u8", "")
                        new JSONChannel("401",
                                "OutOfFocus.TV",
                                "http://pablogott.videocdn.scaleengine.net/pablogott-iphone/play/ooftv1/playlist.m3u8",
                                "http://i.imgur.com/QRCIhN4.png", "",
                                TvContract.Programs.Genres.MUSIC),
                        new JSONChannel("900",
                                "Artbeats Demo",
                                "http://cdn-fms.rbs.com.br/hls-vod/sample1_1500kbps.f4v.m3u8", "", "",
                                TvContract.Programs.Genres.ARTS),
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
                if(i == null) {
                    Toast.makeText(MainActivity.this, "Is Live Channels installed? Email felker.tech@gmail.com", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(i);
                }
            }
        });
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
        Log.d(TAG, "onConnected");
        if(md != null) {
            if(md.isShowing()) {
                md.dismiss();
            }
        }
        sm.setGoogleDriveSyncable(gapi, new SettingsManager.GoogleDriveListener() {
            @Override
            public void onActionFinished(boolean cloudToLocal) {
                Log.d(TAG, "Sync req after drive action");
                final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
                SyncUtils.requestSync(info);
                if (cloudToLocal) {
                    Toast.makeText(MainActivity.this, "Download complete", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Upload complete", Toast.LENGTH_SHORT).show();
                }
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
            if(!gapi.isConnected()) {
                md = new MaterialDialog.Builder(this)
                        .customView(R.layout.load_dialog, false)
                        .show();

                gapi.connect();
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
        }
        /*md = new MaterialDialog.Builder(this)
                .customView(R.layout.load_dialog, false)
                .show();*/
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
        Log.d(TAG, "Error connecting " + connectionResult.getErrorCode());
        if(md != null) {
            if (md.isShowing()) {
                ((TextView) md.getCustomView().findViewById(R.id.message)).setText("Error connecting: " + connectionResult.toString());
            }
        }
        Log.d(TAG, "oCF " + connectionResult.toString());
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
        String[] actions = new String[] {"Browse Plugins",
                "Switch Google Drive file",
                "Refresh Data - Cloud to Local",
                "View Software Licenses",
                "Reset Channel Data",
                "Graphics credited to bgiesing from GitHub",
                "Read XMLTV (EXPERIMENTAL)"};
        new MaterialDialog.Builder(MainActivity.this)
                .title("More Actions")
                .items(actions)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        switch (i) {
                            case 0:
                                //Same opening
                                final PackageManager pm = getPackageManager();
                                final Intent plugin_addchannel = new Intent("com.felkertech.cumulustv.ADD_CHANNEL");
                                final List<ResolveInfo> plugins = pm.queryIntentActivities(plugin_addchannel, 0);
                                ArrayList<String> plugin_names = new ArrayList<String>();
                                for (ResolveInfo ri : plugins) {
                                    plugin_names.add(ri.loadLabel(pm).toString());
                                }
                                String[] plugin_names2 = plugin_names.toArray(new String[plugin_names.size()]);

                                new MaterialDialog.Builder(MainActivity.this)
                                        .title("Installed Plugins")
                                        .items(plugin_names2)
                                        .itemsCallback(new MaterialDialog.ListCallback() {
                                            @Override
                                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                                //Load the given plugin with some additional info
                                                ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                                String s = cd.toString();
                                                Intent intent = new Intent();
                                                Log.d(TAG, "Try to start");
                                                ResolveInfo plugin_info = plugins.get(i);
                                                Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                                                            plugin_info.activityInfo.name);

                                                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                                                        plugin_info.activityInfo.name);
                                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EXTRA_READ_ALL);
                                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ALL_CHANNELS, s);
                                                startActivity(intent);
                                            }
                                        })
                                        .positiveText("Download More")
                                        .callback(new MaterialDialog.ButtonCallback() {
                                            @Override
                                            public void onPositive(MaterialDialog dialog) {
                                                super.onPositive(dialog);
                                                Intent i = new Intent(Intent.ACTION_VIEW);
                                                i.setData(Uri.parse("http://play.google.com/store/search?q=cumulustv&c=apps"));
                                                startActivity(i);
                                            }
                                        }).show();
                                break;
                            case 1:
//                                Toast.makeText(MainActivity.this, "Not supported", Toast.LENGTH_SHORT).show();
                                if (gapi.isConnected()) {
                                    IntentSender intentSender = Drive.DriveApi
                                            .newOpenFileActivityBuilder()
                                            .setMimeType(new String[]{"application/json", "text/*"})
                                            .build(gapi);
                                    try {
                                        startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.w(TAG, "Unable to send intent", e);
                                    }
                                } else {
                                    Toast.makeText(MainActivity.this, "Please wait until Drive Service is active", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 2:
                                readDriveData();
                                break;
                            case 3:
                                oslClick();
                                break;
                            case 4:
                                new MaterialDialog.Builder(MainActivity.this)
                                        .title("Delete all your channel data?")
                                        .positiveText("Yes")
                                        .negativeText("NO")
                                        .callback(new MaterialDialog.ButtonCallback() {
                                            @Override
                                            public void onPositive(MaterialDialog dialog) {
                                                super.onPositive(dialog);
                                                sm.setString(ChannelDatabase.KEY, "{'channels':[]}");
                                                try {
                                                    DriveId did = DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id));
                                                    sm.writeToGoogleDrive(did,
                                                            sm.getString(ChannelDatabase.KEY));
                                                } catch (Exception e) {
                                                    Toast.makeText(MainActivity.this, "Error: DriveId is invalid", Toast.LENGTH_SHORT).show();
                                                }
                                                sm.setString(R.string.sm_google_drive_id, "");
                                                Toast.makeText(MainActivity.this, "The deed was done", Toast.LENGTH_SHORT).show();
                                                Intent i = new Intent(MainActivity.this, MainActivity.class);
                                                startActivity(i);
                                            }
                                        })
                                        .show();
                                break;
                            case 5:
                                Intent gi = new Intent(Intent.ACTION_VIEW);
                                gi.setData(Uri.parse("http://github.com/fleker/cumulustv"));
                                startActivity(gi);
                                break;
                            case 6:
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
    public void readDriveData() {
        DriveId did;
        try {
             did = DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id));
        } catch (Exception e) {
            Toast.makeText(this, "Invalid drive file. Please choose a different file.", Toast.LENGTH_SHORT).show();
            return;
        }
        sm.readFromGoogleDrive(did, ChannelDatabase.KEY);

        final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
        SyncUtils.requestSync(info);
    }
    public void writeDriveData() {
        try {
            sm.writeToGoogleDrive(DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id)), new ChannelDatabase(this).toString());

            final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
            SyncUtils.requestSync(info);
        } catch(Exception e) {
            //Probably invalid drive id. No worries, just let someone know
            Toast.makeText(this, "Invalid drive file. Please choose a different file.", Toast.LENGTH_SHORT).show();
        }
    }

    /* PLUGIN INTERFACES */
    public void openPluginPicker(final boolean newChannel) {
        openPluginPicker(newChannel, 0);
    }
    public void openPluginPicker(final boolean newChannel, final int index) {
        final PackageManager pm = getPackageManager();
        final Intent plugin_addchannel = new Intent("com.felkertech.cumulustv.ADD_CHANNEL");
        final List<ResolveInfo> plugins = pm.queryIntentActivities(plugin_addchannel, 0);
        ArrayList<String> plugin_names = new ArrayList<String>();
        for (ResolveInfo ri : plugins) {
            plugin_names.add(ri.loadLabel(pm).toString());
        }
        String[] plugin_names2 = plugin_names.toArray(new String[plugin_names.size()]);
        Log.d(TAG, "Load plugins "+plugin_names.toString());
        if(plugin_names.size() == 1) {
            Intent intent = new Intent();
            if (newChannel) {
                Log.d(TAG, "Try to start ");
                ResolveInfo plugin_info = plugins.get(0);
                Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                        plugin_info.activityInfo.name);

                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                        plugin_info.activityInfo.name);
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_ADD);
            } else {
                ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
                try {
                    JSONChannel jsonChannel = new JSONChannel(cdn.getJSONChannels().getJSONObject(index));
                    ResolveInfo plugin_info = plugins.get(0);
                    Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                            plugin_info.activityInfo.name);
                    intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                            plugin_info.activityInfo.name);
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NUMBER, jsonChannel.getNumber());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NAME, jsonChannel.getName());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_URL, jsonChannel.getUrl());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ICON, jsonChannel.getLogo());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_SPLASH, jsonChannel.getSplashscreen());
                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_GENRES, jsonChannel.getGenresString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            startActivity(intent);
        } else {
            new MaterialDialog.Builder(MainActivity.this)
                    .items(plugin_names2)
                    .title("Choose an app")
                    .content("Yes, if there's only one app, default to that one")
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                            Toast.makeText(getApplicationContext(), "Pick " + i, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent();
                            if (newChannel) {
                                Log.d(TAG, "Try to start ");
                                ResolveInfo plugin_info = plugins.get(i);
                                Log.d(TAG, plugin_info.activityInfo.applicationInfo.packageName + " " +
                                        plugin_info.activityInfo.name);

                                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                                        plugin_info.activityInfo.name);

                                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_ADD);
                            } else {
                                ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
                                try {
                                    JSONChannel jsonChannel = new JSONChannel(cdn.getJSONChannels().getJSONObject(index));
                                    ResolveInfo plugin_info = plugins.get(i);
                                    intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                                            plugin_info.activityInfo.name);
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NUMBER, jsonChannel.getNumber());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_NAME, jsonChannel.getName());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_URL, jsonChannel.getUrl());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ICON, jsonChannel.getLogo());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_SPLASH, jsonChannel.getSplashscreen());
                                    intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_GENRES, jsonChannel.getGenresString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            startActivity(intent);
                        }
                    }).show();
        }
    }
}
