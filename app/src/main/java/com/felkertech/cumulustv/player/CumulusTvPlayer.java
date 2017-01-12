package com.felkertech.cumulustv.player;

import android.content.Context;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
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
public class CumulusTvPlayer implements TvPlayer, ExoPlayer.EventListener {
    private static final String TAG = CumulusTvPlayer.class.getSimpleName();
    private static final boolean DEBUG = false;

    private List<Callback> mTvCallbacks = new ArrayList<>();
    private List<ErrorListener> mErrorListeners = new ArrayList<>();
    private SimpleExoPlayer mSimpleExoPlayer;
    private float mPlaybackSpeed;
    private Context mContext;

    public CumulusTvPlayer(Context context) {
        this(context,  new DefaultTrackSelector(), new DefaultLoadControl());
    }

    public CumulusTvPlayer(Context context, TrackSelector trackSelector, LoadControl loadControl) {
        mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
        mContext = context;
        mSimpleExoPlayer.addListener(this);
        mSimpleExoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
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
        if (DEBUG) {
            Log.d(TAG, "Set params " + params.toString());
        }
    }

    public float getPlaybackSpeed() {
        return mPlaybackSpeed;
    }

    @Override
    public long getCurrentPosition() {
        return mSimpleExoPlayer.getBufferedPosition();
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
        mSimpleExoPlayer.setVolume(volume);
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

    public void registerErrorListener(ErrorListener callback) {
        mErrorListeners.add(callback);
    }

    public void unregisterErrorListener(ErrorListener callback) {
        mErrorListeners.remove(callback);
    }

    public void startPlaying(Uri mediaUri) {
        // This is the MediaSource representing the media to be played.
        try {
            MediaSource videoSource = MediaSourceFactory.getMediaSourceFor(mContext, mediaUri);
            // Prepare the player with the source.
            mSimpleExoPlayer.prepare(videoSource);
        } catch (MediaSourceFactory.NotMediaException e) {
            for (ErrorListener listener : mErrorListeners) {
                listener.onError(e);
            }
        }
    }

    public void stop() {
        mSimpleExoPlayer.stop();
    }

    public void release() {
        mSimpleExoPlayer.removeListener(this);
        mSimpleExoPlayer.release();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        for (Callback tvCallback : mTvCallbacks) {
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                tvCallback.onCompleted();
            } else if (playWhenReady && playbackState == ExoPlayer.STATE_READY) {
                tvCallback.onStarted();
            }
        }
        Log.d(TAG, "Player state changed to " + playbackState + ", PWR: " + playWhenReady);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        for (Callback tvCallback : mTvCallbacks) {
            tvCallback.onError();
        }
        for (ErrorListener listener : mErrorListeners) {
            listener.onError(error);
        }
    }

    @Override
    public void onPositionDiscontinuity() {
    }

    public interface ErrorListener {
        void onError(Exception error);
    }
}
