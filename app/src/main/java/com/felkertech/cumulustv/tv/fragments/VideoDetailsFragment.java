package com.felkertech.cumulustv.tv.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.n.cumulustv.R;
import com.felkertech.cumulustv.activities.MainActivity;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.tv.Utils;
import com.felkertech.cumulustv.tv.activities.DetailsActivity;
import com.felkertech.cumulustv.tv.activities.LeanbackActivity;
import com.felkertech.cumulustv.tv.presenters.DetailsDescriptionPresenter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class VideoDetailsFragment extends DetailsFragment
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = VideoDetailsFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final String EXTRA_JSON_CHANNEL = "json";

    private static final int ACTION_ADD = 3;
    private static final int ACTION_EDIT = 5;
    private static final int ACTION_WATCH = 6;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private static final int NUM_COLS = 10;

    private JsonChannel jsonChannel;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;

    private GoogleApiClient gapi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        prepareBackgroundManager();

        try {
            jsonChannel = new JsonChannel.Builder(getActivity().getIntent()
                    .getStringExtra(EXTRA_JSON_CHANNEL))
                    .build();
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        if (jsonChannel != null) {
            setupAdapter();
            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            setupMovieListRowPresenter();
            updateBackground();
        } else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }

        gapi = new GoogleApiClient.Builder(getActivity())
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        gapi.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.c_background5);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    protected void updateBackground() {
        mBackgroundManager.setDrawable(getResources().getDrawable(R.drawable.c_background5));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBackground();
    }

    private void setupAdapter() {
        mPresenterSelector = new ClassPresenterSelector();
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    private void setupDetailsOverviewRow() {
        final DetailsOverviewRow row = new DetailsOverviewRow(jsonChannel);
        row.setImageDrawable(getResources().getDrawable(R.drawable.c_background5));
        int width = Utils.convertDpToPixel(getActivity()
                .getApplicationContext(), DETAIL_THUMB_WIDTH);
        int height = Utils.convertDpToPixel(getActivity()
                .getApplicationContext(), DETAIL_THUMB_HEIGHT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bitmap = Glide.with(getActivity())
                            .load(ChannelDatabase.getNonNullChannelLogo(jsonChannel))
                            .asBitmap()
                            .fitCenter()
                            .error(R.drawable.c_background5)
                            .into(DETAIL_THUMB_WIDTH, DETAIL_THUMB_HEIGHT)
                            .get();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            row.setImageBitmap(getActivity(), bitmap);
                            mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                        }
                    });
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        ArrayObjectAdapter actions = new ArrayObjectAdapter();
        // Add another action IF it isn't a channel you already have:
        ChannelDatabase cdn = ChannelDatabase.getInstance(getActivity());
        if(cdn.findChannelByMediaUrl(jsonChannel.getMediaUrl()) == null) {
            actions.add(new Action(ACTION_ADD, getString(R.string.add_channel_txt)));
        } else {
            actions.add(new Action(ACTION_EDIT, getString(R.string.edit_channel)));
        }
        actions.add(new Action(ACTION_WATCH, getString(R.string.play)));
        row.setActionsAdapter(actions);
        mAdapter.add(row);
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background and style.
        DetailsOverviewRowPresenter detailsPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        detailsPresenter.setBackgroundColor(getResources().getColor(R.color.selected_background));
        detailsPresenter.setStyleLarge(true);

        // Hook up transition element.
        detailsPresenter.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if(action.getId() == ACTION_EDIT) {
                    ActivityUtils.editChannel(getActivity(), jsonChannel.getMediaUrl());
                } else if(action.getId() == ACTION_WATCH) {
                    Log.d(TAG, ChannelDatabase.getInstance(getActivity()).getHashMap().toString());
                    if (ChannelDatabase.getInstance(getActivity()).getHashMap()
                            .containsKey(jsonChannel.getMediaUrl())) {
                        // Open in Live Channels
                        Uri liveChannelsUri =
                                TvContract.buildChannelUri(
                                        ChannelDatabase.getInstance(
                                                getActivity()).getHashMap()
                                                .get(jsonChannel.getMediaUrl()));
                        getActivity().startActivity(
                                new Intent(Intent.ACTION_VIEW, liveChannelsUri));
                    } else {
                        ActivityUtils.openStream(getActivity(), jsonChannel.getMediaUrl());
                    }
                } else if(action.getId() == ACTION_ADD) {
                    Log.d(TAG, "Adding " + jsonChannel.toString());
                    ActivityUtils
                            .addChannel(getActivity(), gapi, jsonChannel);
                    getActivity().setResult(LeanbackActivity.RESULT_CODE_REFRESH_UI);
                    getActivity().finish();
                }
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }

    private void setupMovieListRowPresenter() {
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
