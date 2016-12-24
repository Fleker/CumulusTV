package com.felkertech.cumulustv.player;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

/**
 * Created by guest1 on 12/23/2016.
 */

public class MediaSourceFactory {
    public static MediaSource getMediaSourceFor(Context context, Uri mediaUri) {
        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "yourApplicationName"), bandwidthMeter);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        String url = mediaUri.toString();
        if (url.endsWith("m3u8")) {
            // HLS
            return new HlsMediaSource(mediaUri, dataSourceFactory, null, null);
        } else if (url.endsWith("rtmp")) {
            throw new RuntimeException("Cannot play RTMP stream " + mediaUri);
        } else {
            return new ExtractorMediaSource(mediaUri, dataSourceFactory, extractorsFactory, null,
                    null);
        }
    }
}
