package com.felkertech.n.cumulustv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.example.android.sampletvinput.player.TvInputPlayer;
import com.example.android.sampletvinput.player.WebTvPlayer;
import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.model.Program;
import com.felkertech.channelsurfer.service.MultimediaInputProvider;
import com.felkertech.channelsurfer.service.SimpleSessionImpl;
import com.felkertech.channelsurfer.service.TvInputProvider;
import com.google.android.exoplayer.ExoPlaybackException;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import io.fabric.sdk.android.Fabric;

/**
 * Created by N on 7/12/2015.
 */
public class SampleTvInput extends MultimediaInputProvider {
    HandlerThread mHandlerThread;
    BroadcastReceiver mBroadcastReceiver;
    Handler mDbHandler;
    CaptioningManager mCaptioningManager;
    SimpleSessionImpl session;
    String TAG = "cumulus:TvInputService";
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mHandlerThread = new HandlerThread(getClass()
                .getSimpleName());
        Fabric.with(this, new Crashlytics());
        mHandlerThread.start();
        mDbHandler = new Handler(mHandlerThread.getLooper());
        mCaptioningManager = (CaptioningManager)
                getSystemService(Context.CAPTIONING_SERVICE);

        setTheme(R.style.Theme_AppCompat_NoActionBar);

//        mSessions = new ArrayList<BaseTvInputSessionImpl>();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager
                .ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager
                .ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public List<Channel> getAllChannels() {
        ChannelDatabase cdn = new ChannelDatabase(this);
        try {
            return cdn.getChannels();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Program> getProgramsForChannel(Uri channelUri, Channel channelInfo, long startTimeMs, long endTimeMs) {
        return null;
    }

    @Override
    public boolean onSetSurface(Surface surface) {
        return false;
    }

    @Override
    public void onSetStreamVolume(float volume) {

    }

    @Override
    public void onRelease() {

    }

    @Override
    public View onCreateVideoView() {
        return null;
    }

    @Override
    public boolean onTune(Channel channel) {
        return false;
    }

    @Nullable
    @Override
    public Session onCreateSession(String inputId) {
        session = new CumulusSession(this, this);
        Log.d(TAG, "Start session "+inputId);
        session.setOverlayViewEnabled(true);
        return session;
    }

    /**
     * Simple session implementation which plays local videos on the application's tune request.
     */
    private class CumulusSession extends SimpleSessionImpl {
        TvInputPlayer exoPlayer;
        private String TAG = "cumulus:TIS.S";
        private boolean isWeb = false;
        private JSONChannel jsonChannel;
        CumulusSession(Context context, TvInputProvider tvInputProvider) {
            super(context, tvInputProvider);
        }
        @Override
        public View onCreateOverlayView() {
            LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(!isWeb) {
                try {
                    final View v = inflater.inflate(R.layout.loading, null);
//            v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.slide_in_left));
                    if (jsonChannel == null) {
                        ((TextView) v.findViewById(R.id.channel)).setText("");
                        ((TextView) v.findViewById(R.id.title)).setText("");
                    } else if (jsonChannel.hasSplashscreen()) {
                        ImageView iv = new ImageView(getApplicationContext());
                        Picasso.with(getApplicationContext()).load(jsonChannel.getSplashscreen()).into(iv);
                        return iv;
                    } else {
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
                                            ((ImageView) v.findViewById(R.id.thumnail)).setImageBitmap(bitmap[0]);

                                            //Use Palette to grab colors
                                            Palette p = Palette.from(bitmap[0])
                                                    .generate();
                                            if (p.getVibrantSwatch() != null) {
                                                Log.d(TAG, "Use vibrant");
                                                Palette.Swatch s = p.getVibrantSwatch();
                                                v.setBackgroundColor(s.getRgb());
                                                ((TextView) v.findViewById(R.id.channel)).setTextColor(s.getTitleTextColor());
                                                ((TextView) v.findViewById(R.id.title)).setTextColor(s.getTitleTextColor());
                                                ((TextView) v.findViewById(R.id.channel_msg)).setTextColor(s.getTitleTextColor());

                                                //Now style the progress bar
                                                if (p.getDarkVibrantSwatch() != null) {
                                                    Palette.Swatch dvs = p.getDarkVibrantSwatch();
                                                    ((ProgressWheel) v.findViewById(R.id.indeterminate_progress_large_library)).setBarColor(dvs.getRgb());
                                                }
                                            } else if (p.getDarkVibrantSwatch() != null) {
                                                Log.d(TAG, "Use dark vibrant");
                                                Palette.Swatch s = p.getDarkVibrantSwatch();
                                                v.setBackgroundColor(s.getRgb());
                                                ((TextView) v.findViewById(R.id.channel)).setTextColor(s.getTitleTextColor());
                                                ((TextView) v.findViewById(R.id.title)).setTextColor(s.getTitleTextColor());
                                                ((TextView) v.findViewById(R.id.channel_msg)).setTextColor(s.getTitleTextColor());
                                                ((ProgressWheel) v.findViewById(R.id.indeterminate_progress_large_library)).setBarColor(s.getRgb());
                                            } else if (p.getSwatches().size() > 0) {
                                                //Go with default if no vibrant swatch exists
                                                Log.d(TAG, "No vibrant swatch, " + p.getSwatches().size() + " others");
                                                Palette.Swatch s = p.getSwatches().get(0);
                                                v.setBackgroundColor(s.getRgb());
                                                ((TextView) v.findViewById(R.id.channel)).setTextColor(s.getTitleTextColor());
                                                ((TextView) v.findViewById(R.id.title)).setTextColor(s.getTitleTextColor());
                                                ((TextView) v.findViewById(R.id.channel_msg)).setTextColor(s.getTitleTextColor());
                                                ((ProgressWheel) v.findViewById(R.id.indeterminate_progress_large_library)).setBarColor(s.getBodyTextColor());
                                            }
                                        }
                                    };
                                    try {
                                        if (jsonChannel.getLogo() != null && !jsonChannel.getLogo().isEmpty()) {
                                            bitmap[0] = Picasso.with(getApplicationContext())
                                                    .load(jsonChannel.getLogo())
                                                    .placeholder(R.drawable.ic_launcher)
                                                    .get();
                                            h.sendEmptyMessage(0);
                                        } //Else we have no set logo
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
//                            .into((ImageView) v.findViewById(R.id.thumnail));
                        }
                    }
                    Log.d(TAG, "Overlay");
                    return v;
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "The loading screen can't seem to open, but the channel is loading.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Failure to open: " + e.getMessage());
                    return null;
                }
            } else {
                //Website
                WebTvPlayer webView = new WebTvPlayer(getApplicationContext());
                webView.load(jsonChannel.getUrl());
                return webView;
            }
        }
        public boolean tuneTo(String channel_no) {
            ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
            JSONChannel ch = cdn.findChannel(channel_no);
            String stream;
            jsonChannel = ch;
            if(jsonChannel != null) {
                stream = ch.getUrl();
                return startPlayback(stream);
            }
            return false;
        }
        @Override
        public boolean onTune(Uri channelUri) {
            Log.d(TAG, "onTune");
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
            setOverlayViewEnabled(true);
            String[] projection = {TvContract.Channels.COLUMN_SERVICE_ID, TvContract.Channels.COLUMN_INPUT_ID, TvContract.Channels.COLUMN_DISPLAY_NUMBER};
            String stream = "";
            Log.d(TAG, "Tuning to "+channelUri.toString());
            Cursor cursor = null;
            //Now look up this channel in the DB
            try {
                cursor = getContentResolver().query(channelUri, projection, null, null, null);
                if (cursor == null || cursor.getCount() == 0) {
                    return false;
                }
                cursor.moveToNext();
                String tv_no = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER));
                ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
                JSONChannel ch = cdn.findChannel(tv_no);
                jsonChannel = ch;
                if(jsonChannel != null) {
                    if(jsonChannel.hasService()) {
                        PackageManager pm = getPackageManager();
                        final String pack = jsonChannel.getService().split(",")[0];
                        boolean app_installed = false;
                        try {
                            pm.getPackageInfo(pack, PackageManager.GET_ACTIVITIES);
                            app_installed = true;
                            //Open up this particular activity
                            Intent intent = new Intent();
                            intent.setClassName(pack,
                                    jsonChannel.getService().split(",")[1]);
                            startService(intent);
                            Handler h = new Handler(Looper.getMainLooper()) {
                                @Override
                                public void handleMessage(Message msg) {
                                    super.handleMessage(msg);
                                    tuneTo(jsonChannel.getNumber());
                                }
                            };
                            h.sendEmptyMessageDelayed(0, 5000);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        stream = ch.getUrl();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Channel "+tv_no+" not found", Toast.LENGTH_SHORT).show();
                    notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                }
                Log.d(TAG, "Retrieved stream "+stream);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            Log.d(TAG, "Tune into " + channelUri.toString());
            return startPlayback(stream);
            // NOTE: To display the program information (e.g. title) properly in the channel banner,
            // The implementation needs to register the program metadata on TvProvider.
            // For the example implementation, please see {@link RichTvInputService}.
        }
        private boolean startPlayback(String url) {
            setOverlayViewEnabled(true);
            final String ETAG = "cumulus:exoplayer";
            notifyVideoUnavailable(
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
            Log.d(TAG, "Found channel & start playing "+url);
            if(exoPlayer == null) {
                exoPlayer = new TvInputPlayer();
                exoPlayer.addCallback(new TvInputPlayer.Callback() {
                    @Override
                    public void onPrepared() {
                        Log.d(ETAG, "Player onPrepared");
                    }

                    @Override
                    public void onPlayerStateChanged(boolean playWhenReady, int state) {
                        if(state == TvInputPlayer.STATE_BUFFERING) {
                            notifyVideoUnavailable(
                                    TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
                            Log.d(ETAG, "Buffering");
                            notifyVideoAvailable();
                            setOverlayViewEnabled(true);
                        } else if(state == TvInputPlayer.STATE_READY) {
                            Handler h = new Handler(Looper.myLooper()) {
                                @Override
                                public void handleMessage(Message msg) {
                                    super.handleMessage(msg);
                                    notifyVideoAvailable();
                                    setOverlayViewEnabled(false);
                                    Log.d(ETAG, "Now playing");
                                }
                            };
                            /*notifyVideoUnavailable(
                                    TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);*/
                            notifyVideoAvailable();
//                            h.sendEmptyMessageDelayed(0, 2000);
                            h.sendEmptyMessageDelayed(0, 0);
                            setOverlayViewEnabled(true);
                            Log.d(ETAG, "Ready");
                        } else if(state == TvInputPlayer.STATE_IDLE) {
                            Log.d(ETAG, "Idle");
                        } else if(state == TvInputPlayer.STATE_ENDED) {
                            Log.d(ETAG, "Ended");
                        } else if(state == TvInputPlayer.STATE_PREPARING) {
                            notifyVideoUnavailable(
                                    TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
                            Log.d(ETAG, "Preparing");
                            setOverlayViewEnabled(false);
                        }

                        /*else if(state == TvInputPlayer.STATE_IDLE) {
                            notifyVideoAvailable();
                            setOverlayViewEnabled(false);
                        }*/
                    }

                    @Override
                    public void onPlayWhenReadyCommitted() {
                        Log.d(ETAG, "Play when ready committed");
                    }

                    @Override
                    public void onPlayerError(ExoPlaybackException e) {
                        Log.d(TAG, "An error occurs!");
                        Toast.makeText(getApplicationContext(), "Error occurred with this channel: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        notifyVideoUnavailable(
                                TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                        setOverlayViewEnabled(true);
                        Log.d(TAG, e.getMessage());
                    }

                    @Override
                    public void onDrawnToSurface(Surface surface) {
                        /*notifyVideoAvailable();
                        setOverlayViewEnabled(false);*/
                        Log.d(ETAG, "onDrawnToSurface");
                    }

                    @Override
                    public void onText(String text) {

                    }
                });
                exoPlayer.setSurface(mSurface);
                exoPlayer.setVolume(mVolume);
            }
            exoPlayer.addCallback(new TvInputPlayer.Callback() {
                @Override
                public void onPrepared() {

                }

                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int state) {

                }

                @Override
                public void onPlayWhenReadyCommitted() {

                }

                @Override
                public void onPlayerError(ExoPlaybackException e) {
                    Log.e(TAG, "Error preparing player: "+e.getLocalizedMessage());
                    if(e.getMessage().contains("Extractor")) {
                        Log.d(TAG, "Cannot play the stream, try loading it as a website");
                        //Pretend this is a website
                        setOverlayViewEnabled(true);
                        notifyVideoAvailable();
                        isWeb = true;
                    }
                }

                @Override
                public void onDrawnToSurface(Surface surface) {

                }

                @Override
                public void onText(String text) {

                }
            });
            exoPlayer.prepare(getApplicationContext(), Uri.parse(url), TvInputPlayer.SOURCE_TYPE_HLS);
            exoPlayer.setPlayWhenReady(true);
            return true;
        }
    }
}
