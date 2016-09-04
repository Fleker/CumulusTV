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

package com.felkertech.n.tv.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.felkertech.channelsurfer.sync.SyncUtils;
import com.felkertech.n.ActivityUtils;
import com.felkertech.n.boilerplate.Utils.AppUtils;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JsonChannel;
import com.felkertech.n.cumulustv.model.Option;
import com.felkertech.n.cumulustv.model.SuggestedChannels;
import com.felkertech.n.cumulustv.receivers.GoogleDriveBroadcastReceiver;
import com.felkertech.n.tv.activities.DetailsActivity;
import com.felkertech.n.tv.presenters.CardPresenter;
import com.felkertech.n.tv.presenters.OptionsCardPresenter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.MetadataChangeSet;

import org.json.JSONException;

import java.util.Timer;
import java.util.TimerTask;

public class LeanbackFragment extends BrowseFragment
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = LeanbackFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    public static final int REQUEST_CODE_CREATOR = 102;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private BackgroundManager mBackgroundManager;
    private DriveSettingsManager sm;
    public GoogleApiClient gapi;
    public Activity mActivity;
    private GoogleDriveBroadcastReceiver broadcastReceiver = new GoogleDriveBroadcastReceiver() {
        @Override
        public void onDownloadCompleted() {
            refreshUI();
        }

        @Override
        public void onUploadCompleted() {

        }

        @Override
        public void onNetworkActionCompleted() {
            if (DEBUG) {
                Log.d(TAG, "Some network action occurred");
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);
        sm = new DriveSettingsManager(getActivity());
        ActivityUtils.GoogleDrive.autoConnect(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshUI();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver,
                new IntentFilter(GoogleDriveBroadcastReceiver.ACTION_STATUS_CHANGED));
    }

    public void refreshUI() {
        prepareBackgroundManager();
        setupUIElements();
        loadRows();
        setupEventListeners();
    }

    private void loadRows() {
        // Here are my rows
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        // My channels
        if(mActivity == null && Build.VERSION.SDK_INT >= 23) {
            Toast.makeText(getContext(), R.string.toast_error_no_activity, Toast.LENGTH_SHORT)
                    .show();
            return;
        } else if(mActivity == null) {
            return;
        }
        ChannelDatabase cd = ChannelDatabase.getInstance(mActivity);
        try {
            CardPresenter channelCardPresenter = new CardPresenter();
            ArrayObjectAdapter channelRowAdapter = new ArrayObjectAdapter(channelCardPresenter);
            int index = 0;
            for(JsonChannel jsonChannel : cd.getJsonChannels()) {
                if (DEBUG) {
                    Log.d(TAG, "Got channels " + jsonChannel.getName());
                    Log.d(TAG, jsonChannel.getLogo());
                }
                channelRowAdapter.add(jsonChannel);
                index++;
            }
            HeaderItem header = new HeaderItem(0, getString(R.string.my_channels));
            mRowsAdapter.add(new ListRow(header, channelRowAdapter));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Second row is suggested channels
        CardPresenter suggestedChannelPresenter = new CardPresenter();
        ArrayObjectAdapter suggestedChannelAdapter =
                new ArrayObjectAdapter(suggestedChannelPresenter);
        HeaderItem suggestedChannelsHeader = new HeaderItem(1,
                getString(R.string.suggested_channels));
        JsonChannel[] suggestedChannels = SuggestedChannels.getSuggestedChannels();
        for(JsonChannel jsonChannel : suggestedChannels) {
            suggestedChannelAdapter.add(jsonChannel);
        }
        mRowsAdapter.add(new ListRow(suggestedChannelsHeader, suggestedChannelAdapter));

        // Third row is Drive
        HeaderItem driveHeader = new HeaderItem(1, getString(R.string.google_drive_sync));
        OptionsCardPresenter drivePresenter = new OptionsCardPresenter();
        ArrayObjectAdapter driveAdapter = new ArrayObjectAdapter(drivePresenter);
        driveAdapter.add(new Option(getResources().getDrawable(R.drawable.ic_google_drive),
                getString(R.string.connect_drive)));
        driveAdapter.add(new Option(getResources().getDrawable(R.drawable.ic_cloud_download),
                getString(R.string.settings_refresh_cloud_local)));
        driveAdapter.add(new Option(getResources().getDrawable(R.drawable.ic_google_drive_folder),
                getString(R.string.settings_switch_google_drive)));
        mRowsAdapter.add(new ListRow(driveHeader, driveAdapter));

        // Fourth row is actions
        HeaderItem gridHeader = new HeaderItem(1, getString(R.string.manage));
        OptionsCardPresenter mGridPresenter = new OptionsCardPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(new Option(getResources().getDrawable(R.drawable.ic_television),
                getString(R.string.manage_livechannels)));
        gridRowAdapter.add(new Option(getResources().getDrawable(R.drawable.ic_airplay),
                getString(R.string.manage_add_new)));
        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        // Settings will become its own activity
        HeaderItem gridHeader2 = new HeaderItem(1, getString(R.string.settings));
        OptionsCardPresenter mGridPresenter2 = new OptionsCardPresenter();
        ArrayObjectAdapter gridRowAdapter2 = new ArrayObjectAdapter(mGridPresenter2);
        /*
        FIXME Plugin browser hidden due to #165
        gridRowAdapter2.add(new Option(getResources().getDrawable(R.drawable.ic_animation),
                    getString(R.string.settings_browse_plugins)));
                    */
        gridRowAdapter2.add(new Option(getResources().getDrawable(R.drawable.ic_book_open),
                getString(R.string.settings_view_licenses)));
        gridRowAdapter2.add(new Option(getResources().getDrawable(R.drawable.ic_delete_forever),
                getString(R.string.settings_reset_channel_data)));
        gridRowAdapter2.add(new Option(getResources().getDrawable(R.drawable.ic_help_circle_fill),
                getString(R.string.about_app)));
        mRowsAdapter.add(new ListRow(gridHeader2, gridRowAdapter2));

        setAdapter(mRowsAdapter);
    }

    private void prepareBackgroundManager() {
        try {
            mBackgroundManager = BackgroundManager.getInstance(getActivity());
            mBackgroundManager.attach(getActivity().getWindow());
            mDefaultBackground = getResources().getDrawable(R.drawable.c_background5);
            mBackgroundManager.setDrawable(getResources().getDrawable(R.drawable.c_background5));
            mMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        } catch(Exception ignored) {
        }
    }

    private void setupUIElements() {
        try {
            setBadgeDrawable(getActivity().getResources().getDrawable(R.mipmap.ic_launcher));
            setTitle(getString(R.string.app_name)); // Badge, when set, takes precedent
            // over title
            setHeadersState(HEADERS_ENABLED);
            setHeadersTransitionOnBackEnabled(true);

            // set fastLane (or headers) background color
            setBrandColor(getResources().getColor(R.color.colorPrimary));
            // set search icon color
//        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
        } catch(Exception ignored) {
        }
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

        sm.setGoogleDriveSyncable(gapi, new DriveSettingsManager.GoogleDriveListener() {
            @Override
            public void onActionFinished(boolean cloudToLocal) {
                Log.d(TAG, "Sync req after drive action");
                final String info = TvContract.buildInputId(ActivityUtils.TV_INPUT_SERVICE);
                SyncUtils.requestSync(mActivity, info);
                if (cloudToLocal) {
                    Toast.makeText(getActivity(), R.string.download_complete, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.upload_complete, Toast.LENGTH_SHORT).show();
                }
            }
        }); //Enable GDrive
        Log.d(TAG, sm.getString(R.string.sm_google_drive_id) + "<< for onConnected");
        if(sm.getString(R.string.sm_google_drive_id).isEmpty()) {
            //We need a new file
            ActivityUtils.createDriveData(mActivity, gapi, driveContentsCallback);
        } else {
            //Great, user already has sync enabled, let's resync
            ActivityUtils.readDriveData(mActivity, gapi);
            Handler h = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    refreshUI();
                }
            };
            h.sendEmptyMessageDelayed(0, 4000);
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
                            .build(ActivityUtils.GoogleDrive.gapi);
                    try {
                        mActivity.startIntentSenderForResult(
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
        if (DEBUG) {
            Log.d(TAG, "Error connecting " + connectionResult.getErrorCode());
            Log.d(TAG, "oCF " + connectionResult.toString());
        }
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(mActivity, ActivityUtils.RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), mActivity, 0).show();
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof JsonChannel) {
                JsonChannel jsonChannel = (JsonChannel) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(mActivity, DetailsActivity.class);
                intent.putExtra(VideoDetailsFragment.EXTRA_JSON_CHANNEL, jsonChannel.toString());

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        mActivity,
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                mActivity.startActivity(intent, bundle);
            } else if (item instanceof Option) {
                String title = ((Option) item).getText();
                if(title.equals(getString(R.string.manage_livechannels))) {
                    ActivityUtils.launchLiveChannels(mActivity);
                } else if(title.equals(getString(R.string.manage_add_suggested))) {
                   ActivityUtils.openSuggestedChannels(mActivity, gapi);
                } else if(title.equals(getString(R.string.manage_add_new))) {
                    ActivityUtils.openPluginPicker(true, mActivity);
                } else if(title.equals(getString(R.string.connect_drive))) {
                    ActivityUtils.GoogleDrive.connect(mActivity);
                } else if(title.equals(getString(R.string.settings_switch_google_drive))) {
                    ActivityUtils.GoogleDrive.pickDriveFile(mActivity);
                } else if(title.equals(getString(R.string.settings_browse_plugins))) {
                    ActivityUtils.browsePlugins(mActivity);
                } else if(title.equals(getString(R.string.settings_switch_google_drive))) {
                    ActivityUtils.switchGoogleDrive(mActivity, gapi);
                } else if(title.equals(getString(R.string.settings_refresh_cloud_local))) {
                    ActivityUtils.readDriveData(mActivity, gapi);
                    Handler h = new Handler(Looper.myLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            refreshUI();
                        }
                    };
                    h.sendEmptyMessageDelayed(0, 4000);
                } else if(title.equals(getString(R.string.settings_view_licenses))) {
                    ActivityUtils.oslClick(mActivity);
                } else if(title.equals(getString(R.string.settings_reset_channel_data))) {
                    ActivityUtils.deleteChannelData(mActivity, gapi);
                } else if(title.equals(getString(R.string.about_app))) {
                    ActivityUtils.openAbout(mActivity);
                } else {
                    Toast.makeText(mActivity, ((String) item), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof JsonChannel) {
                startBackgroundTimer();
            }

        }
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
            Log.d(TAG, item.toString());
            ((TextView) viewHolder.view).setText(item.toString());
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }
}
