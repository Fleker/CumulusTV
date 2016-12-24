package com.felkertech.cumulustv.tv;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Point;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;

import com.felkertech.cumulustv.services.CumulusJobService;
import com.felkertech.n.cumulustv.R;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.media.tv.companionlibrary.BaseTvInputService;
import com.google.android.media.tv.companionlibrary.EpgSyncJobService;
import com.google.android.media.tv.companionlibrary.TvPlayer;
import com.google.android.media.tv.companionlibrary.model.Advertisement;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.model.RecordedProgram;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guest1 on 12/23/2016.
 */

public class CumulusTvTifService extends BaseTvInputService {
    private static final String TAG = "RichTvInputService";
    private static final boolean DEBUG = false;
    private static final long EPG_SYNC_DELAYED_PERIOD_MS = 1000 * 2; // 2 Seconds

    private CaptioningManager mCaptioningManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mCaptioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
    }

    @Override
    public final Session onCreateSession(String inputId) {
        RichTvInputSessionImpl session = new RichTvInputSessionImpl(this, inputId);
        session.setOverlayViewEnabled(true);
        return super.sessionCreated(session);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public TvInputService.RecordingSession onCreateRecordingSession(String inputId) {
        return null;
    }

    class RichTvInputSessionImpl extends BaseTvInputService.Session implements
            DemoPlayer.Listener, DemoPlayer.CaptionListener {
        private static final float CAPTION_LINE_HEIGHT_RATIO = 0.0533f;
        private static final int TEXT_UNIT_PIXELS = 0;
        private static final String UNKNOWN_LANGUAGE = "und";

        private int mSelectedSubtitleTrackIndex;
        private SubtitleLayout mSubtitleView;
        private DemoPlayer mPlayer;
        private boolean mCaptionEnabled;
        private String mInputId;
        private Context mContext;

        RichTvInputSessionImpl(Context context, String inputId) {
            super(context, inputId);
            mCaptionEnabled = mCaptioningManager.isEnabled();
            mContext = context;
            mInputId = inputId;
        }

        @Override
        public View onCreateOverlayView() {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            return null;
        }

        private List<TvTrackInfo> getAllTracks() {
            String trackId;
            List<TvTrackInfo> tracks = new ArrayList<>();

            int[] trackTypes = {
                    DemoPlayer.TYPE_AUDIO,
                    DemoPlayer.TYPE_VIDEO,
                    DemoPlayer.TYPE_TEXT
            };

            for (int trackType : trackTypes) {
                int count = mPlayer.getTrackCount(trackType);
                for (int i = 0; i < count; i++) {
                    MediaFormat format = mPlayer.getTrackFormat(trackType, i);
                    trackId = getTrackId(trackType, i);
                    TvTrackInfo.Builder builder = new TvTrackInfo.Builder(trackType, trackId);

                    if (trackType == DemoPlayer.TYPE_VIDEO) {
                        if (format.maxWidth != MediaFormat.NO_VALUE) {
                            builder.setVideoWidth(format.maxWidth);
                        } else if (format.width != MediaFormat.NO_VALUE) {
                            builder.setVideoWidth(format.width);
                        }
                        if (format.maxHeight != MediaFormat.NO_VALUE) {
                            builder.setVideoHeight(format.maxHeight);
                        } else if (format.height != MediaFormat.NO_VALUE) {
                            builder.setVideoHeight(format.height);
                        }
                    } else if (trackType == DemoPlayer.TYPE_AUDIO) {
                        builder.setAudioChannelCount(format.channelCount);
                        builder.setAudioSampleRate(format.sampleRate);
                        if (format.language != null && !UNKNOWN_LANGUAGE.equals(format.language)) {
                            // TvInputInfo expects {@code null} for unknown language.
                            builder.setLanguage(format.language);
                        }
                    } else if (trackType == DemoPlayer.TYPE_TEXT) {
                        if (format.language != null && !UNKNOWN_LANGUAGE.equals(format.language)) {
                            // TvInputInfo expects {@code null} for unknown language.
                            builder.setLanguage(format.language);
                        }
                    }

                    tracks.add(builder.build());
                }
            }
            return tracks;
        }

        @Override
        public boolean onPlayProgram(Program program, long startPosMs) {
            if (program == null) {
                requestEpgSync(getCurrentChannelUri());
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
                return false;
            }
            createPlayer(program.getInternalProviderData().getVideoType(),
                    Uri.parse(program.getInternalProviderData().getVideoUrl()));
            if (startPosMs > 0) {
                mPlayer.seekTo(startPosMs);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
            }
            mPlayer.setPlayWhenReady(true);
            return true;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public boolean onPlayRecordedProgram(RecordedProgram recordedProgram) {
            createPlayer(recordedProgram.getInternalProviderData().getVideoType(),
                    Uri.parse(recordedProgram.getInternalProviderData().getVideoUrl()));

            long recordingStartTime = recordedProgram.getInternalProviderData()
                    .getRecordedProgramStartTime();
            mPlayer.seekTo(recordingStartTime - recordedProgram.getStartTimeUtcMillis());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
            }
            mPlayer.setPlayWhenReady(true);
            return true;
        }

        public TvPlayer getTvPlayer() {
            return mPlayer;
        }

        @Override
        public boolean onTune(Uri channelUri) {
            if (DEBUG) {
                Log.d(TAG, "Tune to " + channelUri.toString());
            }
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
            releasePlayer();
            return super.onTune(channelUri);
        }

        @Override
        public void onPlayAdvertisement(Advertisement advertisement) {
            createPlayer(TvContractUtils.SOURCE_TYPE_HTTP_PROGRESSIVE,
                    Uri.parse(advertisement.getRequestUrl()));
        }

        private void createPlayer(int videoType, Uri videoUrl) {
            releasePlayer();
            mPlayer = new DemoPlayer(RendererBuilderFactory.createRendererBuilder(
                    mContext, videoType, videoUrl));
            mPlayer.addListener(this);
            mPlayer.setCaptionListener(this);
            mPlayer.prepare();
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            mCaptionEnabled = enabled;
            if (mPlayer != null) {
                if (mCaptionEnabled) {
                    mPlayer.setSelectedTrack(TvTrackInfo.TYPE_SUBTITLE,
                            mSelectedSubtitleTrackIndex);
                } else {
                    mPlayer.setSelectedTrack(TvTrackInfo.TYPE_SUBTITLE, DemoPlayer.TRACK_DISABLED);
                }
            }
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            if (trackId == null) {
                return true;
            }

            int trackIndex = getIndexFromTrackId(trackId);
            if (mPlayer != null) {
                if (type == TvTrackInfo.TYPE_SUBTITLE) {
                    if (! mCaptionEnabled) {
                        return false;
                    }
                    mSelectedSubtitleTrackIndex = trackIndex;
                }

                mPlayer.setSelectedTrack(type, trackIndex);
                notifyTrackSelected(type, trackId);
                return true;
            }
            return false;
        }

        private void releasePlayer() {
            if (mPlayer != null) {
                mPlayer.removeListener(this);
                mPlayer.setSurface(null);
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }
        }

        @Override
        public void onRelease() {
            super.onRelease();
            releasePlayer();
        }

        @Override
        public void onBlockContent(TvContentRating rating) {
            super.onBlockContent(rating);
            releasePlayer();
        }

        private float getCaptionFontSize() {
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            Point displaySize = new Point();
            display.getSize(displaySize);
            return Math.max(getResources().getDimension(R.dimen.subtitle_minimum_font_size),
                    CAPTION_LINE_HEIGHT_RATIO * Math.min(displaySize.x, displaySize.y));
        }

        @Override
        public void onStateChanged(boolean playWhenReady, int playbackState) {
            if (mPlayer == null) {
                return;
            }

            if (playWhenReady && playbackState == ExoPlayer.STATE_READY) {
                notifyTracksChanged(getAllTracks());
                String audioId = getTrackId(TvTrackInfo.TYPE_AUDIO,
                        mPlayer.getSelectedTrack(TvTrackInfo.TYPE_AUDIO));
                String videoId = getTrackId(TvTrackInfo.TYPE_VIDEO,
                        mPlayer.getSelectedTrack(TvTrackInfo.TYPE_VIDEO));
                String textId = getTrackId(TvTrackInfo.TYPE_SUBTITLE,
                        mPlayer.getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE));

                notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, audioId);
                notifyTrackSelected(TvTrackInfo.TYPE_VIDEO, videoId);
                notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, textId);
                notifyVideoAvailable();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Math.abs(mPlayer.getPlaybackSpeed() - 1) < 0.1 &&
                    playWhenReady && playbackState == ExoPlayer.STATE_BUFFERING) {
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
            }
        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                       float pixelWidthHeightRatio) {
            // Do nothing.
        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, e.getMessage());
        }

        @Override
        public void onCues(List<Cue> cues) {
            mSubtitleView.setCues(cues);
        }

        public void requestEpgSync(final Uri channelUri) {
            EpgSyncJobService.requestImmediateSync(CumulusTvTifService.this, mInputId,
                    new ComponentName(CumulusTvTifService.this, CumulusJobService.class));
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    onTune(channelUri);
                }
            }, EPG_SYNC_DELAYED_PERIOD_MS);
        }
    }
}
