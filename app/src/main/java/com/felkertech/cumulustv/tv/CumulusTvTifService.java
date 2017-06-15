package com.felkertech.cumulustv.tv;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.player.CumulusTvPlayer;
import com.felkertech.cumulustv.player.CumulusWebPlayer;
import com.felkertech.cumulustv.player.MediaSourceFactory;
import com.felkertech.cumulustv.services.CumulusJobService;
import com.felkertech.n.cumulustv.R;
import com.google.android.media.tv.companionlibrary.BaseTvInputService;
import com.google.android.media.tv.companionlibrary.TvPlayer;
import com.google.android.media.tv.companionlibrary.model.Advertisement;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.model.RecordedProgram;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


/**
 * An instance of {@link BaseTvInputService} which plays Cumulus Tv videos.
 */
public class CumulusTvTifService extends BaseTvInputService {
    private static final String TAG = CumulusTvTifService.class.getSimpleName();
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

    class RichTvInputSessionImpl extends BaseTvInputService.Session {
        private static final float CAPTION_LINE_HEIGHT_RATIO = 0.0533f;
        private static final int TEXT_UNIT_PIXELS = 0;
        private static final String UNKNOWN_LANGUAGE = "und";

        private int mSelectedSubtitleTrackIndex;
        private CumulusTvPlayer mPlayer;
        private boolean mCaptionEnabled;
        private String mInputId;
        private Context mContext;
        private boolean stillTuning;
        private JsonChannel jsonChannel;
        private long tuneTime;
        private boolean isWeb;

        RichTvInputSessionImpl(Context context, String inputId) {
            super(context, inputId);
            mCaptionEnabled = mCaptioningManager.isEnabled();
            mContext = context;
            mInputId = inputId;
        }

        @Override
        public View onCreateOverlayView() {
            Log.d(TAG, "Create overlay view");
            LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            try {
                final View v = inflater.inflate(R.layout.loading, null);
                if(!stillTuning && jsonChannel.isAudioOnly()) {
                    if (DEBUG) {
                        Log.d(TAG, "Audio-only stream, show a foreground");
                    }
                    ((TextView) v.findViewById(R.id.channel_msg)).setText(R.string.streaming_audio);
                }
                if (DEBUG) {
                    Log.d(TAG, "Trying to load some visual display");
                }
                if (jsonChannel == null) {
                    if (DEBUG) {
                        Log.w(TAG, "Cannot find channel");
                    }
                    ((TextView) v.findViewById(R.id.channel)).setText("");
                    ((TextView) v.findViewById(R.id.title)).setText("");
                } else if (isWeb) {
                    CumulusWebPlayer wv = new CumulusWebPlayer(getApplicationContext(),
                            new CumulusWebPlayer.WebViewListener() {
                                @Override
                                public void onPageFinished() {
                                    //Don't do anything
                                }
                            });
                    wv.load(jsonChannel.getMediaUrl());
                    return wv;
                } else if (jsonChannel.hasSplashscreen()) {
                    if (DEBUG) {
                        Log.d(TAG, "User supplied splashscreen");
                    }
                    ImageView iv = new ImageView(getApplicationContext());
                    Glide.with(getApplicationContext()).load(jsonChannel.getSplashscreen()).into(iv);
                    return iv;
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "Manually create a splashscreen");
                    }
                    ((TextView) v.findViewById(R.id.channel)).setText(jsonChannel.getNumber());
                    ((TextView) v.findViewById(R.id.title)).setText(jsonChannel.getName());
                    if (!jsonChannel.getLogo().isEmpty()) {
                        final Bitmap[] bitmap = {null};
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Handler h = new Handler(Looper.getMainLooper()) {
                                    @Override
                                    public void handleMessage(Message msg) {
                                        super.handleMessage(msg);
                                        ((ImageView) v.findViewById(R.id.thumnail))
                                                .setImageBitmap(bitmap[0]);

                                        //Use Palette to grab colors
                                        Palette p = Palette.from(bitmap[0])
                                                .generate();
                                        if (p.getVibrantSwatch() != null) {
                                            Log.d(TAG, "Use vibrant");
                                            Palette.Swatch s = p.getVibrantSwatch();
                                            v.setBackgroundColor(s.getRgb());
                                            ((TextView) v.findViewById(R.id.channel))
                                                    .setTextColor(s.getTitleTextColor());
                                            ((TextView) v.findViewById(R.id.title))
                                                    .setTextColor(s.getTitleTextColor());
                                            ((TextView) v.findViewById(R.id.channel_msg))
                                                    .setTextColor(s.getTitleTextColor());

                                            //Now style the progress bar
                                            if (p.getDarkVibrantSwatch() != null) {
                                                Palette.Swatch dvs = p.getDarkVibrantSwatch();
                                                ((ProgressWheel) v.findViewById(
                                                        R.id.indeterminate_progress_large_library))
                                                        .setBarColor(dvs.getRgb());
                                            }
                                        } else if (p.getDarkVibrantSwatch() != null) {
                                            Log.d(TAG, "Use dark vibrant");
                                            Palette.Swatch s = p.getDarkVibrantSwatch();
                                            v.setBackgroundColor(s.getRgb());
                                            ((TextView) v.findViewById(R.id.channel))
                                                    .setTextColor(s.getTitleTextColor());
                                            ((TextView) v.findViewById(R.id.title))
                                                    .setTextColor(s.getTitleTextColor());
                                            ((TextView) v.findViewById(R.id.channel_msg))
                                                    .setTextColor(s.getTitleTextColor());
                                            ((ProgressWheel) v.findViewById(
                                                    R.id.indeterminate_progress_large_library))
                                                    .setBarColor(s.getRgb());
                                        } else if (p.getSwatches().size() > 0) {
                                            // Go with default if no vibrant swatch exists
                                            if (DEBUG) {
                                                Log.d(TAG, "No vibrant swatch, " +
                                                        p.getSwatches().size() + " others");
                                            }
                                            Palette.Swatch s = p.getSwatches().get(0);
                                            v.setBackgroundColor(s.getRgb());
                                            ((TextView) v.findViewById(R.id.channel))
                                                    .setTextColor(s.getTitleTextColor());
                                            ((TextView) v.findViewById(R.id.title))
                                                    .setTextColor(s.getTitleTextColor());
                                            ((TextView) v.findViewById(R.id.channel_msg))
                                                    .setTextColor(s.getTitleTextColor());
                                            ((ProgressWheel) v.findViewById(
                                                    R.id.indeterminate_progress_large_library))
                                                    .setBarColor(s.getBodyTextColor());
                                        }
                                    }
                                };
                                try {
                                    bitmap[0] = Glide.with(getApplicationContext())
                                            .load(ChannelDatabase.getNonNullChannelLogo(jsonChannel))
                                            .asBitmap()
                                            .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                            .get();
                                    h.sendEmptyMessage(0);
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "Overlay " + v.toString());
                }
                return v;
            } catch (Exception e) {
                if (DEBUG) {
                    Log.d(TAG, "Failure to open: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public long onTimeShiftGetCurrentPosition() {
            if (mPlayer == null) {
                return TvInputManager.TIME_SHIFT_INVALID_TIME;
            }
            long currentMs = tuneTime + mPlayer.getCurrentPosition();
            if (DEBUG) {
                Log.d(TAG, currentMs + "  " + onTimeShiftGetStartPosition() + " start position");
                Log.d(TAG, (currentMs - onTimeShiftGetStartPosition()) + " diff start position");
            }
            return currentMs;
        }

        @Override
        public boolean onPlayProgram(Program program, long startPosMs) {
            if (program == null) {
                requestEpgSync(getCurrentChannelUri());
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
                return false;
            }
            jsonChannel = ChannelDatabase.getInstance(getApplicationContext()).findChannelByMediaUrl(
                    program.getInternalProviderData().getVideoUrl());
            Log.d(TAG, "Play program " + program.getTitle() + " " +
                    program.getInternalProviderData().getVideoUrl());
            if (program.getInternalProviderData().getVideoUrl() == null) {
                Toast.makeText(mContext, getString(R.string.msg_no_url_found), Toast.LENGTH_SHORT).show();
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                return false;
            } else {
                createPlayer(program.getInternalProviderData().getVideoType(),
                        Uri.parse(program.getInternalProviderData().getVideoUrl()));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
                }
                mPlayer.play();
                notifyVideoAvailable();
                Log.d(TAG, "The video should start playing");
                return true;
            }
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
            mPlayer.play();
            notifyVideoAvailable();
            return true;
        }

        @Override
        public long onTimeShiftGetStartPosition() {
            return tuneTime;
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
            tuneTime = System.currentTimeMillis();
            stillTuning = true;

            // Update our channel
            if (ChannelDatabase.getInstance(mContext).getHashMap() == null) {
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                Toast.makeText(mContext, "Channel HashMap is null", Toast.LENGTH_SHORT).show();
                return false;
            }
            for (String mediaUrl : ChannelDatabase.getInstance(mContext).getHashMap().keySet()) {
                if (channelUri == null) {
                    Toast.makeText(mContext, "channelUri is null", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (ChannelDatabase.getInstance(mContext).getHashMap().get(mediaUrl) ==
                        Long.parseLong(channelUri.getLastPathSegment())) {
                    jsonChannel = ChannelDatabase.getInstance(mContext)
                            .findChannelByMediaUrl(mediaUrl);
                }
            }
            notifyVideoAvailable();
            setOverlayViewEnabled(false);
            setOverlayViewEnabled(true);
            return super.onTune(channelUri);
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            // Captions currently unsupported
        }

        @Override
        public void onPlayAdvertisement(Advertisement advertisement) {
            createPlayer(TvContractUtils.SOURCE_TYPE_HTTP_PROGRESSIVE,
                    Uri.parse(advertisement.getRequestUrl()));
        }

        private void createPlayer(int videoType, Uri videoUrl) {
            releasePlayer();

            mPlayer = new CumulusTvPlayer(mContext);
            mPlayer.registerCallback(new TvPlayer.Callback() {
                @Override
                public void onStarted() {
                    super.onStarted();
                    Log.d(TAG, "Video available");
                    stillTuning = false;
                    notifyVideoAvailable();
                    setOverlayViewEnabled(false);
                    if (jsonChannel != null && jsonChannel.isAudioOnly()) {
                        setOverlayViewEnabled(true);
                    }
                }
            });
            mPlayer.registerErrorListener(new CumulusTvPlayer.ErrorListener() {
                @Override
                public void onError(Exception error) {
                    Log.e(TAG, error.getClass().getSimpleName() + " " + error.getMessage());
                    if (error instanceof MediaSourceFactory.NotMediaException) {
                        isWeb = true;
                        setOverlayViewEnabled(false);
                        setOverlayViewEnabled(true);
                    }
                }
            });
            Log.d(TAG, "Create player for " + videoUrl);
            mPlayer.startPlaying(videoUrl);
        }

        private void releasePlayer() {
            if (mPlayer != null) {
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

        private void requestEpgSync(final Uri channelUri) {
            CumulusJobService.requestImmediateSync1(CumulusTvTifService.this, mInputId, CumulusJobService.DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS,
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
