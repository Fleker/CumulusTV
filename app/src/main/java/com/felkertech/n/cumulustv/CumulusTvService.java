package com.felkertech.n.cumulustv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaDrm;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.model.Program;
import com.felkertech.channelsurfer.service.MultimediaInputProvider;
import com.felkertech.channelsurfer.service.SimpleSessionImpl;
import com.felkertech.n.cumulustv.livechannels.CumulusSessions;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JSONChannel;
import com.felkertech.settingsmanager.SettingsManager;
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
public class CumulusTvService extends MultimediaInputProvider {
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
            Log.d(TAG, "Get program "+channelInfo.getName()+" "+channelInfo.getInternalProviderData());
            programList.add(new Program.Builder(getGenericProgram(channelInfo))
                    .setInternalProviderData(channelInfo.getInternalProviderData())
                    .setCanonicalGenres(new ChannelDatabase(mContext).findChannel(channelInfo.getNumber()).getGenres())
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
            if(!stillTuning && jsonChannel.isAudioOnly()) {
                ((TextView) v.findViewById(R.id.channel_msg)).setText("Playing Radio");
            }
            Log.d(TAG, "Trying to load some visual display");
            if (jsonChannel == null) {
                Log.d(TAG, "Cannot find channel");
                ((TextView) v.findViewById(R.id.channel)).setText("");
                ((TextView) v.findViewById(R.id.title)).setText("");
            } else if (jsonChannel.hasSplashscreen()) {
                Log.d(TAG, "User supplied splashscreen");
                ImageView iv = new ImageView(getApplicationContext());
                Picasso.with(getApplicationContext()).load(jsonChannel.getSplashscreen()).into(iv);
                return iv;
            } else {
                Log.d(TAG, "Manually create a splashscreen");
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
                                if (jsonChannel != null && jsonChannel.getLogo() != null && !jsonChannel.getLogo().isEmpty() && jsonChannel.getLogo().length() > 8) {
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
    private boolean stillTuning;
    @Override
    public boolean onTune(Channel channel) {
        ChannelDatabase cd = new ChannelDatabase(this);
        jsonChannel = cd.findChannel(channel.getNumber());

        Log.d(TAG, "Tune request to go to "+channel.getName());
        Log.d(TAG, "Has IPD of "+channel.getInternalProviderData());
        Log.d(TAG, "Convert to "+jsonChannel.toString());
        if(getProgramRightNow(channel) != null) {
            Log.d(TAG, getProgramRightNow(channel).getInternalProviderData());
            play(getProgramRightNow(channel).getInternalProviderData());
            stillTuning = false;
            if(jsonChannel.isAudioOnly() || jsonChannel.getName().equals("Beats One Radio")) { //Hacky for now
                Log.d(TAG, "Audio only stream");
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        simpleSession.setOverlayViewEnabled(false);
                        simpleSession.setOverlayViewEnabled(true); //Redo splash
                    }
                }.sendEmptyMessageDelayed(0, 150);
            }
            notifyVideoAvailable();
            return true;
        } else {
            Toast.makeText(CumulusTvService.this, "Something's wrong. Cannot tune to this channel.", Toast.LENGTH_SHORT).show();
            notifyVideoUnavailable(REASON_UNKNOWN);
            return false;
        }
    }

    public void onPreTune(Uri channelUri) {
        stillTuning = true;
        Log.d(TAG, "Pre-tune to "+channelUri.getLastPathSegment()+"<");
        Log.d(TAG, new SettingsManager(this).getString("URI"+channelUri.getLastPathSegment())+"<<");
        jsonChannel = new ChannelDatabase(this).findChannel(new SettingsManager(this).getString("URI"+channelUri.getLastPathSegment()));
        simpleSession.setOverlayViewEnabled(false);
        simpleSession.setOverlayViewEnabled(true); //Redo splash
        simpleSession.notifyVideoAvailable();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Toast.makeText(CumulusTvService.this, "You're running low on system memory. Things may not work as expected.", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    @Override
    public Session onCreateSession(String inputId) {
        simpleSession = new CumulusSessions(this);
        Log.d(TAG, "Start session "+inputId);
        simpleSession.setOverlayViewEnabled(true);
        return simpleSession;
    }

}
