/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sampletvinput.player;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.felkertech.n.cumulustv.NotValidExoPlayerStream;
import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.DummyTrackRenderer;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.ExoPlayerLibraryInfo;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.chunk.ChunkSampleSource;
import com.google.android.exoplayer.chunk.ChunkSource;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.FormatEvaluator;
import com.google.android.exoplayer.dash.DashChunkSource;
import com.google.android.exoplayer.dash.DefaultDashTrackSelector;
import com.google.android.exoplayer.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.dash.mpd.Period;
import com.google.android.exoplayer.dash.mpd.Representation;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.text.eia608.Eia608TrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.MimeTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A wrapper around {@link ExoPlayer} that provides a higher level interface. Designed for
 * integration with {@link android.media.tv.TvInputService}.
 */
public class TvInputPlayer implements TextRenderer {
    private static final String TAG = "cumulus:TvInputPlayer";

    public static final int SOURCE_TYPE_HTTP_PROGRESSIVE = 0;
    public static final int SOURCE_TYPE_HLS = 1;
    public static final int SOURCE_TYPE_MPEG_DASH = 2;
    public static final int SOURCE_TYPE_EXTRACTOR = 3; //MPEG-TS, MP3, etc.

    public static final int STATE_IDLE = ExoPlayer.STATE_IDLE;
    public static final int STATE_PREPARING = ExoPlayer.STATE_PREPARING;
    public static final int STATE_BUFFERING = ExoPlayer.STATE_BUFFERING;
    public static final int STATE_READY = ExoPlayer.STATE_READY;
    public static final int STATE_ENDED = ExoPlayer.STATE_ENDED;

    private static final int RENDERER_COUNT = 3;
    private static final int MIN_BUFFER_MS = 1000;
    private static final int MIN_REBUFFER_MS = 5000;

    private int BUFFER_SEGMENT_SIZE = 256 * 1024 * 4;
    private int BUFFER_SEGMENTS = 64 * 2;


    private static final int VIDEO_BUFFER_SEGMENTS = 200;
    private static final int AUDIO_BUFFER_SEGMENTS = 60;
    private static final int LIVE_EDGE_LATENCY_MS = 30000;

    private static final int NO_TRACK_SELECTED = -1;

    private final Handler mHandler;
    private final ExoPlayer mPlayer;
    private TrackRenderer videoRenderer;
    private TrackRenderer audioRenderer;
    private TrackRenderer textRenderer;
    private final CopyOnWriteArrayList<Callback> mCallbacks;
    private float mVolume;
    private Surface mSurface;
    private TvTrackInfo[][] mTvTracks = new TvTrackInfo[RENDERER_COUNT][];
    private int[] mSelectedTvTracks = new int[RENDERER_COUNT];

    private final MediaCodecVideoTrackRenderer.EventListener mVideoRendererEventListener =
            new MediaCodecVideoTrackRenderer.EventListener() {
                @Override
                public void onDroppedFrames(int count, long elapsed) {
                    // Do nothing.
                }

                @Override
                public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

                }

                @Override
                public void onDrawnToSurface(Surface surface) {
                    for(Callback callback : mCallbacks) {
                        callback.onDrawnToSurface(surface);
                    }
                }

                @Override
                public void onDecoderInitializationError(
                        MediaCodecTrackRenderer.DecoderInitializationException e) {
                    for(Callback callback : mCallbacks) {
                        callback.onPlayerError(new ExoPlaybackException(e));
                    }
                }

                @Override
                public void onCryptoError(MediaCodec.CryptoException e) {
                    for(Callback callback : mCallbacks) {
                        callback.onPlayerError(new ExoPlaybackException(e));
                    }
                }

                @Override
                public void onDecoderInitialized(String s, long l, long l1) {

                }
            };

    private final CopyOnWriteArrayList<Callback> callbacks;

    public TvInputPlayer() {
        callbacks = new CopyOnWriteArrayList<>();
        mHandler = new Handler();
        Log.d(TAG, "I'm born!");
        for (int i = 0; i < RENDERER_COUNT; ++i) {
            mTvTracks[i] = new TvTrackInfo[0];
            mSelectedTvTracks[i] = NO_TRACK_SELECTED;
        }
        mCallbacks = new CopyOnWriteArrayList<>();
        mPlayer = ExoPlayer.Factory.newInstance(RENDERER_COUNT, MIN_BUFFER_MS, MIN_REBUFFER_MS);
        mPlayer.addListener(new ExoPlayer.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                for(Callback callback : mCallbacks) {
                    callback.onPlayerStateChanged(playWhenReady, playbackState);
                }
            }

            @Override
            public void onPlayWhenReadyCommitted() {
                for(Callback callback : mCallbacks) {
                    callback.onPlayWhenReadyCommitted();
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException e) {
                for(Callback callback : mCallbacks) {
                    callback.onPlayerError(e);
                }
            }
        });
    }

    public void prepare(final Context context, final Uri originalUri, int sourceType) {
        final String userAgent = getUserAgent(context);
        final DefaultHttpDataSource dataSource = new DefaultHttpDataSource(userAgent, null);
//        final Uri uri = processUriParameters(originalUri, dataSource);
        final Uri uri = originalUri; //Is some sort of url validation to blame?

        if (sourceType == SOURCE_TYPE_HTTP_PROGRESSIVE) {
            Log.d(TAG, "Prep HTTP_PROG");
            ExtractorSampleSource sampleSource =
                    new ExtractorSampleSource(uri, dataSource, new DefaultAllocator(BUFFER_SEGMENT_SIZE),
                            BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE);
            audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);
            videoRenderer = new MediaCodecVideoTrackRenderer(context, sampleSource,
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING, 0, mHandler,
                    mVideoRendererEventListener, 50);
            textRenderer = new DummyTrackRenderer();
            try {
                prepareInternal();
            } catch (Exception e) {

            }
        } else if (sourceType == SOURCE_TYPE_HLS) {
            Log.d(TAG, "Prep HLS");
//            final String userAgent = getUserAgent(context);
            HlsPlaylistParser parser = new HlsPlaylistParser();
//            UriDataSource dataSource = new DefaultUriDataSource(context, userAgent);
            final ManifestFetcher<HlsPlaylist> playlistFetcher =
                    new ManifestFetcher<HlsPlaylist>(uri.toString(), dataSource, parser);
//                    new ManifestFetcher<HlsPlaylist>(parser, uri.toString(), uri.toString(), userAgent);
            playlistFetcher.singleLoad(mHandler.getLooper(),
                    new ManifestFetcher.ManifestCallback<HlsPlaylist>() {
                        @Override
                        public void onSingleManifest(HlsPlaylist hlsPlaylist) {
                            Log.d(TAG, "onSingleManifest(HlsPlaylist)");
                            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
                            DataSource dataSource = new DefaultUriDataSource(context, userAgent);

                            /*HlsChunkSource chunkSource = new HlsChunkSource(dataSource,
                                    uri.toString(), hlsPlaylist, bandwidthMeter, null,
                                    HlsChunkSource.ADAPTIVE_MODE_SPLICE, null);*/
                            HlsChunkSource chunkSource = new HlsChunkSource(dataSource, uri.toString(),
                                    hlsPlaylist, bandwidthMeter,
                                    null, HlsChunkSource.ADAPTIVE_MODE_SPLICE);

                            LoadControl lhc = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
                            HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, lhc, BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE);
                            audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);
                            videoRenderer = new MediaCodecVideoTrackRenderer(context, sampleSource,
                                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING, 5000, mHandler,
                                    mVideoRendererEventListener, 50);
                            textRenderer = new Eia608TrackRenderer(sampleSource,
                                    TvInputPlayer.this, mHandler.getLooper());
                            // TODO: Implement custom HLS source to get the internal track metadata.
                            mTvTracks[TvTrackInfo.TYPE_SUBTITLE] = new TvTrackInfo[1];
                            mTvTracks[TvTrackInfo.TYPE_SUBTITLE][0] =
                                    new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, "1")
                                            .build();
                            prepareInternal();
                        }

                        @Override
                        public void onSingleManifestError(IOException e) {
                            Log.e(TAG, "onSingleManifestError(IOException)");
                            Log.e(TAG, e.getMessage() + "");
                            e.printStackTrace();
//                            Toast.makeText(context, e.getMessage()+"", Toast.LENGTH_SHORT).show();
                            try {
                                prepare(context, originalUri, SOURCE_TYPE_MPEG_DASH);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                            for (Callback callback : mCallbacks) {
                                callback.onPlayerError(new ExoPlaybackException(e));
                            }
                        }
                    });
        } else if (sourceType == SOURCE_TYPE_MPEG_DASH) {
            Log.d(TAG, "Prep MPEG DASH");
            MediaPresentationDescriptionParser parser = new MediaPresentationDescriptionParser();
            final ManifestFetcher<MediaPresentationDescription> manifestFetcher =
                    new ManifestFetcher<>(uri.toString(), dataSource, parser);
            manifestFetcher.singleLoad(mHandler.getLooper(),
                    new ManifestFetcher.ManifestCallback<MediaPresentationDescription>() {
                        @Override
                        public void onSingleManifest(MediaPresentationDescription manifest) {
                            Period period = manifest.getPeriod(0);
                            LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(
                                    BUFFER_SEGMENT_SIZE));

                            // Determine which video representations we should use for playback.
                            int maxDecodableFrameSize;
                            try {
                                maxDecodableFrameSize = MediaCodecUtil.maxH264DecodableFrameSize();
                            } catch (MediaCodecUtil.DecoderQueryException e) {
                                for (Callback callback : callbacks) {
                                    callback.onPlayerError(new ExoPlaybackException(e));
                                }
                                return;
                            }

                            int videoAdaptationSetIndex = period.getAdaptationSetIndex(
                                    AdaptationSet.TYPE_VIDEO);
                            List<Representation> videoRepresentations =
                                    period.adaptationSets.get(videoAdaptationSetIndex).representations;
                            ArrayList<Integer> videoRepresentationIndexList = new ArrayList<>();
                            for (int i = 0; i < videoRepresentations.size(); i++) {
                                Format format = videoRepresentations.get(i).format;
                                if (format.width * format.height > maxDecodableFrameSize) {
                                    // Filtering stream that device cannot play
                                } else if (!format.mimeType.equals(MimeTypes.VIDEO_MP4)
                                        && !format.mimeType.equals(MimeTypes.VIDEO_WEBM)) {
                                    // Filtering unsupported mime type
                                } else {
                                    videoRepresentationIndexList.add(i);
                                }
                            }


                            // Build the video renderer.
                            if (videoRepresentationIndexList.isEmpty()) {
                                videoRenderer = new DummyTrackRenderer();
                            } else {
                                DataSource videoDataSource = new DefaultUriDataSource(context, userAgent);
                                DefaultBandwidthMeter videoBandwidthMeter = new DefaultBandwidthMeter();
                                ChunkSource videoChunkSource = new DashChunkSource(manifestFetcher,
                                        DefaultDashTrackSelector.newVideoInstance(context, true, false),
                                        videoDataSource,
                                        new FormatEvaluator.AdaptiveEvaluator(videoBandwidthMeter), LIVE_EDGE_LATENCY_MS,
                                        0, true, null, null);
                                ChunkSampleSource videoSampleSource = new ChunkSampleSource(
                                        videoChunkSource, loadControl,
                                        VIDEO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE);
                                videoRenderer = new MediaCodecVideoTrackRenderer(context, videoSampleSource,
                                        MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING, 0, mHandler,
                                        mVideoRendererEventListener, 50);
                            }

                            // Build the audio chunk sources.
                            int audioAdaptationSetIndex = period.getAdaptationSetIndex(
                                    AdaptationSet.TYPE_AUDIO);
                            AdaptationSet audioAdaptationSet = period.adaptationSets.get(
                                    audioAdaptationSetIndex);
                            List<ChunkSource> audioChunkSourceList = new ArrayList<>();
                            List<TvTrackInfo> audioTrackList = new ArrayList<>();
                            if (audioAdaptationSet != null) {
                                DataSource audioDataSource = new DefaultUriDataSource(context, userAgent);
                                FormatEvaluator audioEvaluator = new FormatEvaluator.FixedEvaluator();
                                List<Representation> audioRepresentations =
                                        audioAdaptationSet.representations;
                                for (int i = 0; i < audioRepresentations.size(); i++) {
                                    Format format = audioRepresentations.get(i).format;
                                    audioTrackList.add(new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO,
                                            Integer.toString(i))
                                            .setAudioChannelCount(format.audioChannels)
                                            .setAudioSampleRate(format.audioSamplingRate)
                                            .setLanguage(format.language)
                                            .build());
                                    audioChunkSourceList.add(new DashChunkSource(manifestFetcher,
                                            DefaultDashTrackSelector.newAudioInstance(),
                                            audioDataSource,
                                            audioEvaluator, LIVE_EDGE_LATENCY_MS, 0, null, null));
                                }
                            }

                            // Build the audio renderer.
                            //final MultiTrackChunkSource audioChunkSource;
                            if (audioChunkSourceList.isEmpty()) {
                                audioRenderer = new DummyTrackRenderer();
                            } else {
                                //audioChunkSource = new MultiTrackChunkSource(audioChunkSourceList);
                                //SampleSource audioSampleSource = new ChunkSampleSource(audioChunkSource,
                                //        loadControl, AUDIO_BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE);
                                //audioRenderer = new MediaCodecAudioTrackRenderer(audioSampleSource);
                                TvTrackInfo[] tracks = new TvTrackInfo[audioTrackList.size()];
                                audioTrackList.toArray(tracks);
                                mTvTracks[TvTrackInfo.TYPE_AUDIO] = tracks;
                                mSelectedTvTracks[TvTrackInfo.TYPE_AUDIO] = 0;
                                //multiTrackChunkSources[TvTrackInfo.TYPE_AUDIO] = audioChunkSource;
                            }

                            // Build the text renderer.
                            textRenderer = new DummyTrackRenderer();

                            prepareInternal();
                        }

                        @Override
                        public void onSingleManifestError(IOException e) {
                            Log.e(TAG, "MPEG-DASH IOException");
                            Log.e(TAG, e.getMessage());
                            try {
                                prepare(context, originalUri, SOURCE_TYPE_EXTRACTOR);
                            } catch (Exception e1) {
                            }
                            for (Callback callback : callbacks) {
                                callback.onPlayerError(new ExoPlaybackException(e));
                            }
                        }
                    });
        } else if(sourceType == SOURCE_TYPE_EXTRACTOR) {
            Log.d(TAG, "Prep Extractor");
            Log.d(TAG, "Maybe? Fingers crossed.");
           /* throw new NotValidExoPlayerStream();
        } else {*/
            Log.d(TAG, originalUri.toString());
            Log.d(TAG, uri.toString());
            int BUFFER_SEGMENT_SIZE = 64 * 1024;
            int BUFFER_SEGMENT_COUNT = 256;
            Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

            // Build the video and audio renderers.
            try {
                DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(mHandler,
                        null);
                DataSource extractorDataSource = new DefaultUriDataSource(context, bandwidthMeter, userAgent);
                ExtractorSampleSource sampleSource = new ExtractorSampleSource(originalUri, extractorDataSource, allocator,
                        BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
                MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                        sampleSource, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING, 5000, mHandler,
                        mVideoRendererEventListener, 50);
                MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);
                TrackRenderer textRenderer = new DummyTrackRenderer();
                // Invoke the callback.
            /*TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
            renderers[DemoPlayer.TYPE_VIDEO] = videoRenderer;
            renderers[DemoPlayer.TYPE_AUDIO] = audioRenderer;
            renderers[DemoPlayer.TYPE_TEXT] = textRenderer;*/
//            mPlayer.onRenderers(renderers, bandwidthMeter);
                this.videoRenderer = videoRenderer;
                this.audioRenderer = audioRenderer;
                this.textRenderer = textRenderer;
                prepareInternal();
            } catch(Exception e) {

            }
        } /*else {
            throw new IllegalArgumentException("Unknown source type: " + sourceType);
        }*/
    }

    public TvTrackInfo[] getTracks(int trackType) {
        if (trackType < 0 || trackType >= mTvTracks.length) {
            throw new IllegalArgumentException("Illegal track type: " + trackType);
        }
        return mTvTracks[trackType];
    }

    public String getSelectedTrack(int trackType) {
        if (trackType < 0 || trackType >= mTvTracks.length) {
            throw new IllegalArgumentException("Illegal track type: " + trackType);
        }
        if (mSelectedTvTracks[trackType] == NO_TRACK_SELECTED) {
            return null;
        }
        return mTvTracks[trackType][mSelectedTvTracks[trackType]].getId();
    }

    public boolean selectTrack(int trackType, String trackId) {
        if (trackType < 0 || trackType >= mTvTracks.length) {
            return false;
        }
        if (trackId == null) {
            mPlayer.setRendererEnabled(trackType, false);
        } else {
            int trackIndex = Integer.parseInt(trackId);
        }
        return true;
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        mPlayer.setPlayWhenReady(playWhenReady);
    }

    public void setVolume(float volume) {
        mVolume = volume;
        if (mPlayer != null && audioRenderer != null) {
            mPlayer.sendMessage(audioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME,
                    volume);
        }
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
        if (mPlayer != null && videoRenderer != null) {
            mPlayer.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE,
                    surface);
        }
    }

    public void seekTo(long position) {
        mPlayer.seekTo(position);
    }

    public void stop() {
        mPlayer.stop();
    }

    public void release() {
        mPlayer.release();
    }

    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    private void prepareInternal() {
        Log.d(TAG, "Prepare internal");
        try {
            mPlayer.prepare(audioRenderer, videoRenderer, textRenderer);
            mPlayer.sendMessage(audioRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME,
                    mVolume);
            mPlayer.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE,
                    mSurface);
            // Disable text track by default.
            mPlayer.setRendererEnabled(TvTrackInfo.TYPE_SUBTITLE, false);
            for (Callback callback : mCallbacks) {
                callback.onPrepared();
            }
        } catch(Exception E) {
            Log.e(TAG, E.getMessage() + "<(o.o<)");
            Log.e(TAG, E.getClass().getSimpleName());
            if(E.getClass().getName().contains("ExoPlaybackException")) {
                throw new IllegalArgumentException(E.getMessage()+"");
            } else {
//                E.printStackTrace();
            }
        }
    }

    public static String getUserAgent(Context context) {
        String versionName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "?";
        }
        return "SampleTvInput/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE +
                ") " + "ExoPlayerLib/" + ExoPlayerLibraryInfo.VERSION;
    }

    @Override
    public void onCues(List<Cue> list) {
    }
    private static boolean isPlayerPrepared(ExoPlayer player) {
        int state = player.getPlaybackState();
        return state != ExoPlayer.STATE_PREPARING && state != ExoPlayer.STATE_IDLE;
    }

    private static Uri processUriParameters(Uri uri, DefaultHttpDataSource dataSource) {
        String[] parameters = uri.getPath().split("\\|");
        for (int i = 1; i < parameters.length; i++) {
            String[] pair = parameters[i].split("=", 2);
            if (pair.length == 2) {
                dataSource.setRequestProperty(pair[0], pair[1]);
            }
        }

        return uri.buildUpon().path(parameters[0]).build();
    }

    public interface Callback {
        void onPrepared();
        void onPlayerStateChanged(boolean playWhenReady, int state);
        void onPlayWhenReadyCommitted();
        void onPlayerError(ExoPlaybackException e);
        void onDrawnToSurface(Surface surface);
        void onText(String text);
    }
}