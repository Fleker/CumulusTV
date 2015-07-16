package com.felkertech.n.cumulustv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageView;
import android.widget.VideoView;

import com.example.android.sampletvinput.TvContractUtils;
import com.example.android.sampletvinput.player.TvInputPlayer;
import com.example.android.sampletvinput.syncadapter.SyncUtils;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.SubtitleView;

import java.io.IOException;
import java.util.List;

/**
 * Created by N on 7/12/2015.
 */
public class SampleTvInput2 extends TvInputService {
    HandlerThread mHandlerThread;
    BroadcastReceiver mBroadcastReceiver;
//    ArrayList<BaseTvInputSessionImpl> mSessions;
    Handler mDbHandler;
    Handler mHandler;
    CaptioningManager mCaptioningManager;
    String VIDEO_URL = "VIDEOURL";
    SimpleSessionImpl session;
    String TAG = "cumulus:TvInputService";
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mHandlerThread = new HandlerThread(getClass()
                .getSimpleName());
        mHandlerThread.start();
        mDbHandler = new Handler(mHandlerThread.getLooper());
        /*mHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle b = msg.getData();
                String src = b.getString(VIDEO_URL);
                Log.d(TAG, "Got data: "+src+", "+(session != null));
                if(session != null) {
                    session.startPlayback(src);
                }
            }
        };*/
        mCaptioningManager = (CaptioningManager)
                getSystemService(Context.CAPTIONING_SERVICE);

        setTheme(android.R.style.Theme_Holo_Light_NoActionBar);

//        mSessions = new ArrayList<BaseTvInputSessionImpl>();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager
                .ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager
                .ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);

    }

    @Nullable
    @Override
    public Session onCreateSession(String inputId) {
        return new SimpleSessionImpl(this);
        /*session = new SimpleSessionImpl(this);
        Log.d(TAG, "Start session "+inputId);
        return session;*/
    }

    /**
     * Simple session implementation which plays local videos on the application's tune request.
     */
    private class SimpleSessionImpl extends TvInputService.Session {
        private MediaPlayer mPlayer;
        TvInputPlayer exoPlayer;
        private float mVolume;
        private Surface mSurface;
        private String TAG = "cumulus:TIS.S";
        SimpleSessionImpl(Context context) {
            super(context);
        }
        @Override
        public void onRelease() {
            if (exoPlayer != null) {
                exoPlayer.release();
            }
        }
        @Override
        public boolean onSetSurface(Surface surface) {
            if (exoPlayer != null) {
                exoPlayer.setSurface(surface);
            }
            mSurface = surface;
            return true;
        }
        @Override
        public void onSetStreamVolume(float volume) {
            if (exoPlayer != null) {
                exoPlayer.setVolume(volume);
            }
            mVolume = volume;
        }
        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            // The sample content does not have caption. Nothing to do in this sample input.
            // NOTE: If the channel has caption, the implementation should turn on/off the caption
            // based on {@code enabled}.
            // For the example implementation for the case, please see {@link RichTvInputService}.
        }
        @Override
        public View onCreateOverlayView() {
            ImageView iv = new ImageView(getApplicationContext());
            iv.setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher));
            return iv;
        }
        @Override
        public boolean onTune(Uri channelUri) {
            Log.d(TAG, "onTune");
//            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
//            setOverlayViewEnabled(true);
            String[] projection = {TvContract.Channels.COLUMN_SERVICE_ID, TvContract.Channels.COLUMN_INPUT_ID};
            String stream = "http://abclive.abcnews.com/i/abc_live4@136330/index_1200_av-b.m3u8";
            Log.d(TAG, "Tuning to "+channelUri.toString());
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(channelUri, projection, null, null, null);
                if (cursor == null || cursor.getCount() == 0) {
                    return false;
                }
                cursor.moveToNext();
//                stream = cursor.getString(cursor.getColumnIndex());
                Log.d(TAG, "Retrieved stream "+stream);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            Log.d(TAG, "Tune into "+channelUri.toString());
            /*PlayCurrentProgramRunnable mPlayCurrentProgramRunnable = new PlayCurrentProgramRunnable(channelUri);
            mPlayCurrentProgramRunnable.run();*/
            return startPlayback(stream);
//            return true;
            // NOTE: To display the program information (e.g. title) properly in the channel banner,
            // The implementation needs to register the program metadata on TvProvider.
            // For the example implementation, please see {@link RichTvInputService}.
        }
        private boolean startPlayback(String url) {
            setOverlayViewEnabled(true);
            notifyVideoUnavailable(
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
            Log.d(TAG, "Found channel & start playing "+url);
            if(exoPlayer == null) {
                exoPlayer = new TvInputPlayer();
                exoPlayer.addCallback(new TvInputPlayer.Callback() {
                    @Override
                    public void onPrepared() {

                    }

                    @Override
                    public void onPlayerStateChanged(boolean playWhenReady, int state) {
                        if(state == TvInputPlayer.STATE_BUFFERING) {
                            notifyVideoUnavailable(
                                    TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
                            setOverlayViewEnabled(true);
                        } else if(state == TvInputPlayer.STATE_READY) {
                            notifyVideoAvailable();
                            setOverlayViewEnabled(false);
                        }

                        /*else if(state == TvInputPlayer.STATE_IDLE) {
                            notifyVideoAvailable();
                            setOverlayViewEnabled(false);
                        }*/
                    }

                    @Override
                    public void onPlayWhenReadyCommitted() {

                    }

                    @Override
                    public void onPlayerError(ExoPlaybackException e) {
                        Log.d(TAG, "An error occurs!");
                        Log.d(TAG, e.getMessage());
                    }

                    @Override
                    public void onDrawnToSurface(Surface surface) {
                        notifyVideoAvailable();
                        setOverlayViewEnabled(false);
                    }

                    @Override
                    public void onText(String text) {

                    }
                });
                exoPlayer.setSurface(mSurface);
                exoPlayer.setVolume(mVolume);
            }
            exoPlayer.prepare(getApplicationContext(), Uri.parse(url), TvInputPlayer.SOURCE_TYPE_HLS);
            exoPlayer.setPlayWhenReady(true);
            return true;
        }
        private boolean startPlayback(int resource) {
            if (mPlayer == null) {
                mPlayer = new MediaPlayer();
                mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer player, int what, int arg) {
                        // NOTE: TV input should notify the video playback state by using
                        // {@code notifyVideoAvailable()} and {@code notifyVideoUnavailable() so
                        // that the application can display back screen or spinner properly.
                        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                            notifyVideoUnavailable(
                                    TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
                            return true;
                        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END
                                || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            notifyVideoAvailable();
                            return true;
                        }
                        return false;
                    }
                });
                mPlayer.setSurface(mSurface);
                mPlayer.setVolume(mVolume, mVolume);
            } else {
                mPlayer.reset();
            }
            mPlayer.setLooping(true);
            AssetFileDescriptor afd = getResources().openRawResourceFd(resource);
            if (afd == null) {
                return false;
            }
            try {
                mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                        afd.getDeclaredLength());
                mPlayer.prepare();
                mPlayer.start();
            } catch (IOException e) {
                return false;
            } finally {
                try {
                    afd.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
            // The sample content does not have rating information. Just allow the content here.
            // NOTE: If the content might include problematic scenes, it should not be allowed.
            // Also, if the content has rating information, the implementation should allow the
            // content based on the current rating settings by using
            // {@link android.media.tv.TvInputManager#isRatingBlocked()}.
            // For the example implementation for the case, please see {@link RichTvInputService}.
            notifyContentAllowed();
            return true;
        }
    }
    private class PlayCurrentProgramRunnable implements Runnable {
        private static final int RETRY_DELAY_MS = 2000;
        private final Uri mChannelUri;
        private Context mContext;
        private String TAG = "cumulus:SampleTVIpnut2";
        public PlayCurrentProgramRunnable(Uri channelUri) {
            mChannelUri = channelUri;
            mContext = getApplicationContext();
        }

        @Override
        public void run() {
            long nowMs = System.currentTimeMillis();
            List<TvManager.PlaybackInfo> programs = TvContractUtils.getProgramPlaybackInfo(
                    mContext.getContentResolver(), mChannelUri, nowMs, nowMs + 1, 1);
            if (!programs.isEmpty()) {
//                mHandler.removeMessages(MSG_PLAY_PROGRAM);
//                mHandler.obtainMessage(MSG_PLAY_PROGRAM, programs.get(0)).sendToTarget();
                Bundle b = new Bundle();
                b.putString(VIDEO_URL, programs.get(0).videoUrl);
                Message m = new Message();
                m.setData(b);
                mHandler.sendMessage(m);
            } else {
                Log.w(TAG, "Failed to get program info for " + mChannelUri + ". Retry in " +
                        RETRY_DELAY_MS + "ms.");
//                mDbHandler.postDelayed(mPlayCurrentProgramRunnable, RETRY_DELAY_MS);
            }
        }
    }
}
