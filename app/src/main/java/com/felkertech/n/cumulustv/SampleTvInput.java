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
import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.model.Program;
import com.felkertech.channelsurfer.players.TvInputPlayer;
import com.felkertech.channelsurfer.service.MultimediaInputProvider;
import com.felkertech.channelsurfer.service.SimpleSessionImpl;
import com.felkertech.channelsurfer.service.TvInputProvider;
import com.felkertech.channelsurfer.utils.LiveChannelsUtils;
import com.google.android.exoplayer.ExoPlaybackException;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager
                .ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager
                .ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public List<Channel> getAllChannels(Context mContext) {
        ChannelDatabase cdn = new ChannelDatabase(mContext);
        try {
            return cdn.getChannels();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Program> getProgramsForChannel(Context mContext, Uri channelUri, Channel channelInfo, long startTimeMs, long endTimeMs) {
        int programs = (int) ((endTimeMs-startTimeMs)/1000/60/60); //Hour long segments
        int SEGMENT = 1000*60*60; //Hour long segments
        List<Program> programList = new ArrayList<>();
        for(int i=0;i<programs;i++) {
            programList.add(new Program.Builder(getGenericProgram(channelInfo))
                    .setInternalProviderData(channelInfo.getInternalProviderData())
                    .setStartTimeUtcMillis((getNearestHour() + SEGMENT * i))
                    .setEndTimeUtcMillis((getNearestHour() + SEGMENT * (i + 1)))
                    .build()
            );
        }
        return programList;
    }

    @Override
    public View onCreateVideoView() {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        try {
            final View v = inflater.inflate(R.layout.loading, null);
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
                }
            }
            Log.d(TAG, "Overlay");
            return v;
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "The loading screen can't seem to open, but the channel is loading.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Failure to open: " + e.getMessage());
            return null;
        }
    }

    private JSONChannel jsonChannel;
    @Override
    public boolean onTune(Channel channel) {
        ChannelDatabase cd = new ChannelDatabase(this);
        jsonChannel = cd.findChannel(channel.getNumber());

        play(getProgramRightNow(channel).getInternalProviderData());
        notifyVideoAvailable();
        return true;
    }

/*    @Nullable
    @Override
    public Session onCreateSession(String inputId) {
        session = new SimpleSessionImpl(this, this);
        Log.d(TAG, "Start session "+inputId);
        session.setOverlayViewEnabled(true);
        return session;
    }*/
}
