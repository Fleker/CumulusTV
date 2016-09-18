package com.felkertech.n.cumulustv;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.model.Program;
import com.felkertech.channelsurfer.service.MultimediaInputProvider;
import com.felkertech.channelsurfer.sync.SyncAdapter;
import com.felkertech.channelsurfer.sync.SyncUtils;
import com.felkertech.n.ActivityUtils;
import com.felkertech.n.cumulustv.livechannels.CumulusSessions;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JsonChannel;
import com.felkertech.n.fileio.ITvListing;
import com.felkertech.n.fileio.SingleChannel;
import com.felkertech.n.fileio.XmlTvParser;
import com.felkertech.n.tv.activities.PlaybackQuickSettingsActivity;
import com.felkertech.settingsmanager.SettingsManager;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.fabric.sdk.android.Fabric;

/**
 * Created by N on 7/12/2015.
 */
public class CumulusTvService extends MultimediaInputProvider {
    private static final String TAG = CumulusTvService.class.getSimpleName();
    private static final boolean DEBUG = false;

    private HandlerThread mHandlerThread;
    private BroadcastReceiver mBroadcastReceiver;
    private Handler mDbHandler;
    private CaptioningManager mCaptioningManager;
    private JsonChannel jsonChannel;
    private boolean stillTuning;
    private static HashMap<String, XmlTvParser.TvListing> epgData;

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {
            Log.d(TAG, "onCreate");
        }
        mHandlerThread = new HandlerThread(getClass().getSimpleName());
        Fabric.with(this, new Crashlytics());
        mHandlerThread.start();
        mDbHandler = new Handler(mHandlerThread.getLooper());
        mCaptioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void performCustomSync(final SyncAdapter syncAdapter, String inputId) {
        new EpgDataSyncThread(syncAdapter.getContext()).start();
        syncAdapter.performSync(this, inputId);
    }

    @Override
    public List<Channel> getAllChannels(Context context) {
        ChannelDatabase cdn = ChannelDatabase.getInstance(context);
        try {
            List<Channel> channels = cdn.getChannels();
            // Add app linking
            for (Channel channel : channels) {
                channel.setAppLinkText(context.getString(R.string.quick_settings));
                // TODO Swap with cog icon
                channel.setAppLinkIcon("https://github.com/Fleker/CumulusTV/blob/master/app/src/m" +
                        "ain/res/drawable-xhdpi/ic_play_action_normal.png?raw=true");
                channel.setAppLinkPoster(channel.getLogoUrl());
                JsonChannel jsonChannel =
                        cdn.findChannelByMediaUrl(channel.getInternalProviderData());
                channel.setAppLinkIntent(PlaybackQuickSettingsActivity
                        .getIntent(context, jsonChannel).toUri(Intent.URI_INTENT_SCHEME));
            }
            return channels;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Program> getProgramsForChannel(Context context, Uri channelUri,
            Channel channelInfo, long startTimeMs, long endTimeMs) {
        JsonChannel jsonChannel = ChannelDatabase.getInstance(context)
                .findChannelByMediaUrl(channelInfo.getInternalProviderData());
        if (jsonChannel.getEpgUrl() != null && !jsonChannel.getEpgUrl().isEmpty() &&
                epgData.containsKey(jsonChannel.getEpgUrl())) {
            List<Program> programForGivenTime = new ArrayList<>();
            XmlTvParser.TvListing tvListing = epgData.get(jsonChannel.getEpgUrl());
            List<Program> programList = tvListing.getAllPrograms();
            // If repeat-programs is on, schedule the programs sequentially in a loop. To make
            // every device play the same program in a given channel and time, we assumes the
            // loop started from the epoch time.
            long totalDurationMs = 0;
            for (Program program : programList) {
                totalDurationMs += program.getDuration();
            }
            long programStartTimeMs = startTimeMs - startTimeMs % totalDurationMs;
            int i = 0;
            final int programCount = programList.size();
            while (programStartTimeMs < endTimeMs) {
                Program currentProgram = programList.get(i++ % programCount);
                long programEndTimeMs = programStartTimeMs + currentProgram.getDuration();
                if (programEndTimeMs < startTimeMs) {
                    programStartTimeMs = programEndTimeMs;
                    continue;
                }
                programForGivenTime.add(new Program.Builder()
                        .setChannelId(ContentUris.parseId(channelUri))
                        .setTitle(currentProgram.getTitle())
                        .setDescription(currentProgram.getDescription())
                        .setContentRatings(currentProgram.getContentRatings())
                        .setCanonicalGenres(currentProgram.getCanonicalGenres())
                        .setPosterArtUri(currentProgram.getPosterArtUri())
                        .setThumbnailUri(currentProgram.getThumbnailUri())
                        .setInternalProviderData(currentProgram.getInternalProviderData())
                        .setStartTimeUtcMillis(programStartTimeMs)
                        .setEndTimeUtcMillis(programEndTimeMs)
                        .build()
                );
                programStartTimeMs = programEndTimeMs;
            }
            return programForGivenTime;
        } else {
            // Generate fake programs in hour-long segments
            int segment = 1000 * 60 * 60; //Hour long segments
            int programs = (int) ((endTimeMs - startTimeMs) / segment);
            List<Program> programList = new ArrayList<>();
            for(int i = 0; i < programs; i++) {
                if (DEBUG) {
                    Log.d(TAG, "Get program " + channelInfo.getName() + " " +
                            channelInfo.getInternalProviderData());
                }
                programList.add(new Program.Builder(getGenericProgram(channelInfo))
                        .setInternalProviderData(channelInfo.getInternalProviderData())
                        .setCanonicalGenres(ChannelDatabase.getInstance(context)
                                .findChannelByMediaUrl(
                                        channelInfo.getInternalProviderData()).getGenres())
                        .setStartTimeUtcMillis((getNearestHour() + segment * i))
                        .setEndTimeUtcMillis((getNearestHour() + segment * (i + 1)))
                        .build()
                );
            }
            return programList;
        }
    }

    @Override
    public View onCreateVideoView() {
        LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        try {
            final View v = inflater.inflate(R.layout.loading, null);
            if(!stillTuning && jsonChannel.isAudioOnly()) {
                ((TextView) v.findViewById(R.id.channel_msg)).setText("Playing Radio");
            }
            if (DEBUG) {
                Log.d(TAG, "Trying to load some visual display");
            }
            if (jsonChannel == null) {
                if (DEBUG ) {
                    Log.d(TAG, "Cannot find channel");
                }
                ((TextView) v.findViewById(R.id.channel)).setText("");
                ((TextView) v.findViewById(R.id.title)).setText("");
            } else if (jsonChannel.hasSplashscreen()) {
                if (DEBUG) {
                    Log.d(TAG, "User supplied splashscreen");
                }
                ImageView iv = new ImageView(getApplicationContext());
                Picasso.with(getApplicationContext()).load(jsonChannel.getSplashscreen()).into(iv);
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
                                        //Go with default if no vibrant swatch exists
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
                                if (jsonChannel != null && jsonChannel.getLogo() != null &&
                                        !jsonChannel.getLogo().isEmpty() &&
                                        jsonChannel.getLogo().length() > 8) {
                                    bitmap[0] = Picasso.with(getApplicationContext())
                                            .load(jsonChannel.getLogo())
                                            .placeholder(R.mipmap.ic_launcher)
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
            Toast.makeText(getApplicationContext(), R.string.toast_error_no_loading_screen,
                    Toast.LENGTH_SHORT).show();
            if (DEBUG) {
                Log.d(TAG, "Failure to open: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public boolean onTune(final Channel channel) {
        ChannelDatabase cd = ChannelDatabase.getInstance(this);
        jsonChannel = cd.findChannelByMediaUrl(channel.getInternalProviderData());
        if (DEBUG) {
            Log.d(TAG, "Tune request to go to " + channel.getName());
            Log.d(TAG, "Has IPD of " + channel.getInternalProviderData());
            Log.d(TAG, "Convert to " + jsonChannel.toString());
        }
        if(getProgramRightNow(channel) != null) {
            Log.d(TAG, getProgramRightNow(channel).getInternalProviderData());
            play(getProgramRightNow(channel).getInternalProviderData());
            stillTuning = false;
            // Hacky for now
            if(jsonChannel.isAudioOnly()) {
                if (DEBUG) {
                    Log.d(TAG, "Audio only stream");
                }
                Handler refreshLayout = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        if (DEBUG) {
                            Log.d(TAG, "Redraw splash");
                        }
                        simpleSession.setOverlayViewEnabled(false);
                        simpleSession.setOverlayViewEnabled(true); //Redo splash
                    }
                };
                refreshLayout.sendEmptyMessageDelayed(0, 5);
                refreshLayout.sendEmptyMessageDelayed(0, 550);
            }
            notifyVideoAvailable();
            return true;
        } else {
            SyncUtils.requestSync(getApplicationContext(), ActivityUtils.TV_INPUT_SERVICE
                    .flattenToString());
            Toast.makeText(CumulusTvService.this, R.string.toast_error_cannot_tune,
                    Toast.LENGTH_SHORT).show();
            notifyVideoUnavailable(REASON_UNKNOWN);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onTune(channel);
                }
            }, 1000 * 5);
            return false;
        }
    }

    public void onPreTune(Uri channelUri) {
        stillTuning = true;
        if (DEBUG) {
            Log.d(TAG, "Pre-tune to " + channelUri.getLastPathSegment() + "<");
        }
        long rowId = Long.parseLong(channelUri.getLastPathSegment());
        jsonChannel = ChannelDatabase.getInstance(this).getChannelFromRowId(rowId);
        simpleSession.setOverlayViewEnabled(false);
        simpleSession.setOverlayViewEnabled(true); //Redo splash
        simpleSession.notifyVideoAvailable();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Toast.makeText(CumulusTvService.this, R.string.toast_error_low_memory,
                Toast.LENGTH_SHORT).show();
    }

    @Nullable
    @Override
    public Session onCreateSession(String inputId) {
        simpleSession = new CumulusSessions(this);
        if (DEBUG) {
            Log.d(TAG, "Start session " + inputId);
        }
        simpleSession.setOverlayViewEnabled(true);
        return simpleSession;
    }

    private final class EpgDataSyncThread extends Thread {
        private Context mContext;

        EpgDataSyncThread(Context context) {
            super();
            mContext = context;
        }

        @Override
        public void run() {
            super.run();
            epgData = new HashMap<>();
            ChannelDatabase cdn = ChannelDatabase.getInstance(mContext);
            try {
                List<JsonChannel> channels = cdn.getJsonChannels();
                for (JsonChannel jsonChannel : channels) {
                    if (jsonChannel.getEpgUrl() != null && !jsonChannel.getEpgUrl().isEmpty()) {
                        List<Program> programForGivenTime = new ArrayList<>();
                        // Load from the EPG url
                        URLConnection urlConnection = null;
                        try {
                            urlConnection = new URL(jsonChannel.getEpgUrl()).openConnection();
                            urlConnection.setConnectTimeout(1000 * 5);
                            urlConnection.setReadTimeout(1000 * 5);
                            InputStream inputStream = urlConnection.getInputStream();
                            InputStream epgInputStream =  new BufferedInputStream(inputStream);
                            XmlTvParser.TvListing tvListing = XmlTvParser.parse(epgInputStream);
                            epgData.put(jsonChannel.getEpgUrl(), tvListing);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
