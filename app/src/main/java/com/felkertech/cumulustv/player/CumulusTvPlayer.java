package com.felkertech.cumulustv.player;

import android.content.Context;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.media.tv.companionlibrary.TvPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guest1 on 12/23/2016.
 */

public class CumulusTvPlayer implements TvPlayer {
    private List<Callback> mTvCallbacks = new ArrayList<>();
    private SimpleExoPlayer mSimpleExoPlayer;
    private float mPlaybackSpeed;
    private Context mContext;

    public CumulusTvPlayer(Context context, TrackSelector trackSelector, LoadControl loadControl) {
        mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
        mContext = context;
    }

    @Override
    public void seekTo(long position) {
        mSimpleExoPlayer.seekTo(position);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void setPlaybackParams(PlaybackParams params) {
        mSimpleExoPlayer.setPlaybackParams(params);
        mPlaybackSpeed = params.getSpeed();
    }

    public float getPlaybackSpeed() {
        return mPlaybackSpeed;
    }

    @Override
    public long getCurrentPosition() {
        return mSimpleExoPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mSimpleExoPlayer.getDuration();
    }

    @Override
    public void setSurface(Surface surface) {
        mSimpleExoPlayer.setVideoSurface(surface);
    }

    @Override
    public void setVolume(float volume) {

    }

    @Override
    public void pause() {
        mSimpleExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void play() {
        mSimpleExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void registerCallback(Callback callback) {
        mTvCallbacks.add(callback);
    }

    @Override
    public void unregisterCallback(Callback callback) {
        mTvCallbacks.remove(callback);
    }

    public void startPlaying(Uri mediaUri) {
        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(mContext,
                Util.getUserAgent(mContext, "yourApplicationName"), bandwidthMeter);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = new ExtractorMediaSource(mediaUri,
                dataSourceFactory, extractorsFactory, null, null);
        // Prepare the player with the source.
        mSimpleExoPlayer.prepare(videoSource);
    }

    public void stop() {
        mSimpleExoPlayer.stop();
    }

    public void release() {
        mSimpleExoPlayer.release();
    }
}
