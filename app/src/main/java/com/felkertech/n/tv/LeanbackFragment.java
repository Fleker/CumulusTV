/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.felkertech.n.tv;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.android.sampletvinput.syncadapter.SyncUtils;
import com.felkertech.n.ActivityUtils;
import com.felkertech.n.boilerplate.Utils.SettingsManager;
import com.felkertech.n.cumulustv.ChannelDatabase;
import com.felkertech.n.cumulustv.JSONChannel;
import com.felkertech.n.cumulustv.MainActivity;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.TvManager;
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
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

public class LeanbackFragment extends BrowseFragment
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "LeanbackFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;


    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_CREATOR = 102;
    private static final int REQUEST_CODE_OPENER = 104;


    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private URI mBackgroundURI;
    private BackgroundManager mBackgroundManager;

    private SettingsManager sm;
    private GoogleApiClient gapi;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);
        //TODO Get the slides set up
        sm = new SettingsManager(getActivity());
        gapi = new GoogleApiClient.Builder(getActivity())
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        gapi.connect();

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    private void loadRows() {
        //HERE ARE MY ROWS
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        //ROW 1: MY CHANNELS
        ChannelDatabase cd = new ChannelDatabase(getActivity());
        try {
            CardPresenter channelCardPresenter = new CardPresenter();
//            GridItemPresenter channelCardPresenter = new GridItemPresenter();
            ArrayObjectAdapter channelRowAdapter = new ArrayObjectAdapter(channelCardPresenter);
            int index = 0;
            for(TvManager.ChannelInfo channelInfo: cd.getChannels()) {
                Log.d(TAG, "Got channels " + channelInfo.name);
                Log.d(TAG, channelInfo.logoUrl);
                Log.d(TAG, new JSONChannel(cd.getJSONChannels().getJSONObject(index)).toString()+"");
                channelRowAdapter.add(MovieList.buildMovieInfo(
                        "channel",
                        channelInfo.name,
                        "",
                        channelInfo.number,
                        new JSONChannel(cd.getJSONChannels().getJSONObject(index)).getUrl(),
                        channelInfo.logoUrl,
                        channelInfo.logoUrl
                ));
                Log.d(TAG, MovieList.buildMovieInfo(
                        "channel",
                        channelInfo.name,
                        "",
                        channelInfo.number,
                        new JSONChannel(cd.getJSONChannels().getJSONObject(index)).getUrl(),
                        channelInfo.logoUrl,
                        channelInfo.logoUrl
                ).toString());
//                channelRowAdapter.add(channelInfo.name);
                index++;
            }
            HeaderItem header = new HeaderItem(0, "My Channels");
            mRowsAdapter.add(new ListRow(header, channelRowAdapter));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Second row is suggested channels (not really yet)
        HeaderItem suggestedChannelsHeader = new HeaderItem(1, "Suggested Channels");
        GridItemPresenter suggestedChannelsPresenter = new GridItemPresenter();
        ArrayObjectAdapter suggestedRowsAdapter = new ArrayObjectAdapter(suggestedChannelsPresenter);
        suggestedRowsAdapter.add("//TODO");

        //Third row is Drive
        HeaderItem driveHeader = new HeaderItem(1, "Google Drive Sync");
        GridItemPresenter drivePresenter = new GridItemPresenter();
        ArrayObjectAdapter driveAdapter = new ArrayObjectAdapter(drivePresenter);
        driveAdapter.add("Connect to Google Drive");
        driveAdapter.add(getString(R.string.settings_refresh_cloud_local));
        driveAdapter.add("Upload to cloud");
        driveAdapter.add(getString(R.string.settings_switch_google_drive));
        driveAdapter.add(getString(R.string.settings_sync_file));

        //Fourth row are actions
        HeaderItem gridHeader = new HeaderItem(1, "Manage");
        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getString(R.string.manage_livechannels));
        gridRowAdapter.add(getString(R.string.manage_add_suggested));
        gridRowAdapter.add(getString(R.string.manage_add_new));
        gridRowAdapter.add("Empty Plugin");
        gridRowAdapter.add("Settings");
        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        //Settings will become its own activity
        HeaderItem gridHeader2 = new HeaderItem(1, "Settings");
        GridItemPresenter mGridPresenter2 = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter2 = new ArrayObjectAdapter(mGridPresenter2);
        gridRowAdapter2.add(getString(R.string.settings_browse_plugins));
        gridRowAdapter2.add(getString(R.string.settings_view_licenses));
        gridRowAdapter2.add(getString(R.string.settings_reset_channel_data));
        gridRowAdapter2.add(getString(R.string.about_app));
        gridRowAdapter2.add(getString(R.string.settings_read_xmltv));
        mRowsAdapter.add(new ListRow(gridHeader2, gridRowAdapter2));
/*
        List<Movie> list = MovieList.setupMovies();

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();

        int i;
        for (i = 0; i < NUM_ROWS; i++) {
            if (i != 0) {
                Collections.shuffle(list);
            }
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            for (int j = 0; j < NUM_COLS; j++) {
                listRowAdapter.add(list.get(j % 5));
            }
            HeaderItem header = new HeaderItem(i, MovieList.MOVIE_CATEGORY[i]);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        HeaderItem gridHeader = new HeaderItem(i, "PREFERENCES");

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.grid_view));
        gridRowAdapter.add(getString(R.string.error_fragment));
        gridRowAdapter.add(getResources().getString(R.string.personal_settings));
        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));
*/

        setAdapter(mRowsAdapter);

    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
        setTitle(getString(R.string.app_name)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.colorPrimary));
        // set search icon color
//        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
    }

    private void setupEventListeners() {
        /*setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
                        .show();
            }
        });*/

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        sm.setGoogleDriveSyncable(gapi, new SettingsManager.GoogleDriveListener() {
            @Override
            public void onActionFinished(boolean cloudToLocal) {
                Log.d(TAG, "Sync req after drive action");
                final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
                SyncUtils.requestSync(info);
                if (cloudToLocal) {
                    Toast.makeText(getActivity(), "Download complete", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Upload complete", Toast.LENGTH_SHORT).show();
                }
            }
        }); //Enable GDrive
        Log.d(TAG, sm.getString(R.string.sm_google_drive_id)+"<< for onConnected");
        if(sm.getString(R.string.sm_google_drive_id).isEmpty()) {
            //We need a new file
            new MaterialDialog.Builder(getActivity())
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
                        getActivity().startIntentSenderForResult(
                                intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.w(TAG, "Unable to send intent", e);
                    }
                }
            };


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Error connecting " + connectionResult.getErrorCode());

        Log.d(TAG, "oCF " + connectionResult.toString());
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(getActivity(), 900);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), getActivity(), 0).show();
        }
    }
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == getActivity().RESULT_OK) {
                    Log.d(TAG, "App connect +1");
                    gapi.connect();
                } else {
                    Log.d(TAG, "App cannot connect");
                    new MaterialDialog.Builder(getActivity())
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
                ActivityUtils.writeDriveData(getActivity(), gapi);
                break;
            case REQUEST_CODE_OPENER:
                if(data == null) //If op was canceled
                    return;
                driveId = (DriveId) data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                Log.d(TAG, driveId.encodeToString()+", "+driveId.getResourceId()+", "+driveId.toInvariantString());
                sm.setString(R.string.sm_google_drive_id, driveId.encodeToString());
                new MaterialDialog.Builder(getActivity())
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
                                ActivityUtils.writeDriveData(getActivity(), gapi);
                            }
                        })
                        .show();
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.MOVIE, movie);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            } else if (item instanceof String) {
                String title = (String) item;
                if (((String) item).indexOf(getString(R.string.error_fragment)) >= 0) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    startActivity(intent);
                } else if(title.equals(getString(R.string.manage_livechannels))) {
                    Intent i = getActivity().getPackageManager().getLaunchIntentForPackage("com.google.android.tv");
                    if(i == null) {
                        Toast.makeText(getActivity(), "Is Live Channels installed? Email felker.tech@gmail.com", Toast.LENGTH_SHORT).show();
                    } else {
                        startActivity(i);
                    }
                } else if(title.equals(getString(R.string.manage_add_suggested))) {
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
                    };
                    ArrayList<String> channeltext = new ArrayList<String>();
                    for(JSONChannel j: channels) {
                        channeltext.add(j.getName());
                    }
                    final String[] channelList = channeltext.toArray(new String[channeltext.size()]);
                    new MaterialDialog.Builder(getActivity())
                            .title("Here are some suggested streams:")
                            .items(channelList)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                    JSONChannel j = channels[i];
                                    ChannelDatabase cd = new ChannelDatabase(getActivity());
                                    if(cd.channelExists(j)) {
                                        Toast.makeText(getActivity(), "Channel already added", Toast.LENGTH_SHORT).show();
                                    } else {
                                        try {
                                            Toast.makeText(getActivity(), charSequence+" has been added", Toast.LENGTH_SHORT).show();
                                            cd.add(j);
                                            ActivityUtils.writeDriveData(getActivity(), gapi);
//                                        SyncUtils.requestSync(info);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }).show();
                } else if(title.equals(getString(R.string.manage_add_new))) {
                    ActivityUtils.openPluginPicker(true, getActivity());
                } else if(title.equals(getString(R.string.settings_sync_file))) {
                    if (gapi.isConnected()) {
                        IntentSender intentSender = Drive.DriveApi
                                .newOpenFileActivityBuilder()
                                .setMimeType(new String[]{"application/json", "text/*"})
                                .build(gapi);
                        try {
                            getActivity().startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.w(TAG, "Unable to send intent", e);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Please wait until Drive Service is active", Toast.LENGTH_SHORT).show();
                    }
                } else if(title.equals(getString(R.string.settings_browse_plugins))) {
                    //Same opening
                    final PackageManager pm = getActivity().getPackageManager();
                    final Intent plugin_addchannel = new Intent("com.felkertech.cumulustv.ADD_CHANNEL");
                    final List<ResolveInfo> plugins = pm.queryIntentActivities(plugin_addchannel, 0);
                    ArrayList<String> plugin_names = new ArrayList<String>();
                    for (ResolveInfo ri : plugins) {
                        plugin_names.add(ri.loadLabel(pm).toString());
                    }
                    String[] plugin_names2 = plugin_names.toArray(new String[plugin_names.size()]);

                    new MaterialDialog.Builder(getActivity())
                            .title("Installed Plugins")
                            .items(plugin_names2)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                    //Load the given plugin with some additional info
                                    ChannelDatabase cd = new ChannelDatabase(getActivity());
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
                } else if(title.equals(getString(R.string.settings_switch_google_drive))) {
                    if (gapi.isConnected()) {
                        IntentSender intentSender = Drive.DriveApi
                                .newOpenFileActivityBuilder()
                                .setMimeType(new String[]{"application/json", "text/*"})
                                .build(gapi);
                        try {
                            getActivity().startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.w(TAG, "Unable to send intent", e);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Please wait until Drive Service is active", Toast.LENGTH_SHORT).show();
                    }
                } else if(title.equals(getString(R.string.settings_refresh_cloud_local))) {
                    readDriveData();
                } else if(title.equals(getString(R.string.settings_view_licenses))) {
                    ActivityUtils.oslClick(getActivity());
                } else if(title.equals(getString(R.string.settings_reset_channel_data))) {
                    new MaterialDialog.Builder(getActivity())
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
                                        Toast.makeText(getActivity(), "Error: DriveId is invalid", Toast.LENGTH_SHORT).show();
                                    }
                                    sm.setString(R.string.sm_google_drive_id, "");
                                    Toast.makeText(getActivity(), "The deed was done", Toast.LENGTH_SHORT).show();
                                    Intent i = new Intent(getActivity(), LeanbackActivity.class);
                                    startActivity(i);
                                }
                            })
                            .show();
                } else if(title.equals(getString(R.string.about_app))) {
                    Intent gi = new Intent(Intent.ACTION_VIEW);
                    gi.setData(Uri.parse("http://github.com/fleker/cumulustv"));
                    startActivity(gi);
                } else if(title.equals(getString(R.string.settings_read_xmltv))) {
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
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Movie) {
                mBackgroundURI = ((Movie) item).getBackgroundImageURI();
                startBackgroundTimer();
            }

        }
    }

    protected void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        mBackgroundManager.setDrawable(resource);
                    }
                });
        mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundURI != null) {
                        updateBackground(mBackgroundURI.toString());
                    }
                }
            });

        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

    public void readDriveData() {
        DriveId did;
        try {
            did = DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id));
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Invalid drive file. Please choose a different file.", Toast.LENGTH_SHORT).show();
            return;
        }
        sm.readFromGoogleDrive(did, ChannelDatabase.KEY);

        final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
        SyncUtils.requestSync(info);
    }
}
