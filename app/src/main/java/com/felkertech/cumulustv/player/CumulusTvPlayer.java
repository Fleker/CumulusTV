package com.felkertech.cumulustv.player;

import android.content.Context;
import android.media.PlaybackParams;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.trackselection.TrackSelector;
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

    protected CumulusTvPlayer(Context context, TrackSelector trackSelector, LoadControl loadControl)
        {
        mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
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
}
