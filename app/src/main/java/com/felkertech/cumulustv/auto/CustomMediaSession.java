package com.felkertech.cumulustv.auto;

import android.content.Context;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;

import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.player.CumulusTvPlayer;

import org.json.JSONException;

/**
 * Created by Nick on 3/1/2017.
 */

public class CustomMediaSession extends MediaSession.Callback {
    private Context mContext;
    private CumulusTvPlayer mPlayer;

    public CustomMediaSession(Context context) {
        mContext = context;
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
    public void onPrepareFromMediaId(String mediaId, Bundle extras) {
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
        // Actual playing
        if (mPlayer == null) {
            mPlayer = new CumulusTvPlayer(mContext);
        }
        mPlayer.startPlaying(Uri.parse(channel.getMediaUrl()));
    }

    private void startPlaying(String uri) {
        startPlaying(ChannelDatabase.getInstance(mContext).findChannelByMediaUrl(uri));
    }

    @Override
    public void onPause() {
        super.onPause();
        mPlayer.pause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mPlayer.stop();
        mPlayer.release();
    }
}
