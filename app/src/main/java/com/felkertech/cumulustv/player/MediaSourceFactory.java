package com.felkertech.cumulustv.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by guest1 on 12/23/2016.
 */

public class MediaSourceFactory {
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    public static MediaSource getMediaSourceFor(Context context, Uri mediaUri) {
        return getMediaSourceFor(context, mediaUri, null);
    }

    public static MediaSource getMediaSourceFor(Context context, Uri mediaUri,
            String overrideExtension) {
        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "yourApplicationName"), bandwidthMeter);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        String url = mediaUri.toString();
        DataSource.Factory mediaDataSourceFactory = buildDataSourceFactory(context, true);

        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : mediaUri.getLastPathSegment());
        Handler mainHandler = new Handler();
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(mediaUri, buildDataSourceFactory(context, false),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                        mainHandler, null);
            case C.TYPE_DASH:
                return new DashMediaSource(mediaUri, buildDataSourceFactory(context, false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler,
                        null);
            case C.TYPE_HLS:
                return new HlsMediaSource(mediaUri, mediaDataSourceFactory, mainHandler,
                        null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(mediaUri, mediaDataSourceFactory,
                        new DefaultExtractorsFactory(), mainHandler, null);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }

       /* if (url.endsWith("m3u8")) {
            // HLS
            return new HlsMediaSource(mediaUri, dataSourceFactory, null, null);
        } else if (url.endsWith("rtmp")) {
            throw new RuntimeException("Cannot play RTMP stream " + mediaUri);
        } else {
            return new ExtractorMediaSource(mediaUri, dataSourceFactory, extractorsFactory, null,
                    null);
        }*/
    }

    private static DataSource.Factory buildDataSourceFactory(Context context,
            boolean useBandwidthMeter) {
        return buildDataSourceFactory(context, useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    private static HttpDataSource.Factory buildHttpDataSourceFactory(Context context,
             boolean useBandwidthMeter) {
        return buildHttpDataSourceFactory(context, useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    public static DataSource.Factory buildDataSourceFactory(Context context,
             DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(context, bandwidthMeter,
                buildHttpDataSourceFactory(context, bandwidthMeter));
    }

    public static HttpDataSource.Factory buildHttpDataSourceFactory(Context context,
             DefaultBandwidthMeter bandwidthMeter) {
        String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
        return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
    }
}
