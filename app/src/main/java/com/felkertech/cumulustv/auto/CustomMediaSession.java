package com.felkertech.cumulustv.auto;

import android.content.Context;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.player.CumulusTvPlayer;
import com.google.android.media.tv.companionlibrary.TvPlayer;

import org.json.JSONException;

/**
 * Created by Nick on 3/1/2017.
 */

public class CustomMediaSession extends MediaSessionCompat.Callback {
    private static final String TAG = CustomMediaSession.class.getSimpleName();
    private static final boolean DEBUG = true;

    private Context mContext;
    private CumulusTvPlayer mPlayer;
    private Callback mCallback;

    public CustomMediaSession(Context context, Callback callback) {
        mContext = context;
        mCallback = callback;
    }

    @Override
    public void onPlay() {
        try {
            if (ChannelDatabase.getInstance(mContext).getJSONArray().length() > 0) {
                // Play first channel
                startPlaying(ChannelDatabase.getInstance(mContext).getJsonChannels().get(0));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);
        if (DEBUG) {
            Log.d(TAG, "Prepare from media id " + mediaId);
        }
        startPlaying(mediaId); // Media id = url
    }

    @Override
    public void onPlayFromUri(Uri uri, Bundle extras) {
        startPlaying(uri.toString());
    }

    @Override
    public void onPlayFromSearch(String query, Bundle extras) {
        try {
            for (JsonChannel channel : ChannelDatabase.getInstance(mContext).getJsonChannels()) {
                if (channel.getName().contains(query) || channel.getNumber().contains(query)
                        || channel.getGenresString().contains(query)) {
                    startPlaying(channel);
                    return; // Play first match
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startPlaying(JsonChannel channel) {
        if (channel == null) {
            return;
        }
        // Actual playing
        if (mPlayer == null) {
            mPlayer = new CumulusTvPlayer(mContext);
            mPlayer.registerCallback(new TvPlayer.Callback() {
                @Override
                public void onStarted() {
                    super.onStarted();
                    Log.d(TAG, "Playback started");
                }
            });
        }
        if (DEBUG) {
            Log.d(TAG, "Start playing " + channel.getMediaUrl());
        }
        mPlayer.play();
        mPlayer.startPlaying(Uri.parse(channel.getMediaUrl()));
        mCallback.onPlaybackStarted(channel);
    }

    private void startPlaying(String uri) {
        startPlaying(ChannelDatabase.getInstance(mContext).findChannelByMediaUrl(uri));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mCallback.onPlaybackEnded();
        }
    }

    public interface Callback {
        void onPlaybackStarted(JsonChannel channel);
        void onPlaybackEnded();
    }
}
