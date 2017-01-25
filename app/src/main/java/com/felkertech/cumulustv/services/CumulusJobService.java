package com.felkertech.cumulustv.services;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.tv.activities.PlaybackQuickSettingsActivity;
import com.felkertech.cumulustv.utils.AppUtils;
import com.felkertech.n.cumulustv.R;
import com.google.android.media.tv.companionlibrary.EpgSyncJobService;
import com.google.android.media.tv.companionlibrary.XmlTvParser;
import com.google.android.media.tv.companionlibrary.model.Advertisement;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import junit.framework.Assert;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A periodic task that can be run to synchronize data from Google Drive to the system's internal
 * TIF database and local cache.
 */
public class CumulusJobService extends EpgSyncJobService {
    private static final String TAG = CumulusJobService.class.getSimpleName();
    private static final String TEST_AD_REQUEST_URL =
            "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/" +
                    "single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast" +
                    "&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct" +
                    "%3Dlinear&correlator=";
    private static HashMap<String, XmlTvParser.TvListing> epgData;
    public static final long DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS = 1000 * 60 * 60; // 1 Hour

    /**
     * This method needs to be overridden so that we can do asynchronous actions beforehand.
     */
    @Override
    public boolean onStartJob(final JobParameters params) {
        // Broadcast status
        Intent intent = new Intent(ACTION_SYNC_STATUS_CHANGED);
        intent.putExtra(BUNDLE_KEY_INPUT_ID, params.getExtras().getString(BUNDLE_KEY_INPUT_ID));
        Log.d(TAG, "Sync program data for " + params.getExtras().getString(BUNDLE_KEY_INPUT_ID));
        intent.putExtra(SYNC_STATUS, SYNC_STARTED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if (AppUtils.isTV(getApplicationContext())) { // Epg syncing only happens on TVs
            new EpgDataSyncThread(this, new EpgDataCallback() {
                @Override
                public void onComplete() {
                    Log.d(TAG, "Epg data syncing is complete");
                    Handler h = new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            Log.d(TAG, "Run in main thread");
                            EpgSyncTask epgSyncTask = new EpgSyncTask(params);
                            epgSyncTask.execute();
                        }
                    };
                    h.sendEmptyMessage(0);
                }
            }).start();
        }
        return true;
    }

    @Override
    public List<Channel> getChannels() {
        // Build advertisement list for the channel.
        Advertisement channelAd = new Advertisement.Builder()
                .setType(Advertisement.TYPE_VAST)
                .setRequestUrl(TEST_AD_REQUEST_URL)
                .build();
        List<Advertisement> channelAdList = new ArrayList<>();
        channelAdList.add(channelAd);

        InternalProviderData ipd = new InternalProviderData();
//        ipd.setAds(channelAdList);
        ipd.setRepeatable(true);
        ipd.setVideoType(TvContractUtils.SOURCE_TYPE_HLS);

        try {
            List<Channel> channels = ChannelDatabase.getInstance(this).getChannels(ipd);
            // Add app linking
            for (int i = 0; i < channels.size(); i++) {
                JsonChannel jsonChannel =
                        ChannelDatabase.getInstance(this).findChannelByMediaUrl(
                                channels.get(i).getInternalProviderData().getVideoUrl());
                Channel channel = new Channel.Builder(channels.get(i))
                    .setAppLinkText(getString(R.string.quick_settings))
                    .setAppLinkIconUri("https://github.com/Fleker/CumulusTV/blob/master/app/src/m" +
                        "ain/res/drawable-xhdpi/ic_play_action_normal.png?raw=true")
                    .setAppLinkPosterArtUri(channels.get(i).getChannelLogo())
                    .setAppLinkIntent(PlaybackQuickSettingsActivity.getIntent(this, jsonChannel))
                    .build();
                Log.d(TAG, "Adding channel " + channel.getDisplayName());
                channels.set(i, channel);
            }
            Log.d(TAG, "Returning with " + channels.size() + " channels");
            return channels;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.w(TAG, "No channels found");
        return null;
    }

    @Override
    public List<Program> getProgramsForChannel(Uri channelUri, Channel channel, long startMs,
           long endMs) {
        List<Program> programs = new ArrayList<>();
        ChannelDatabase channelDatabase = ChannelDatabase.getInstance(this);
        Log.d(TAG, "Get programs for " + channel.toString());
        JsonChannel jsonChannel = channelDatabase.findChannelByMediaUrl(
                channel.getInternalProviderData().getVideoUrl());

        if (jsonChannel != null && jsonChannel.getEpgUrl() != null &&
                !jsonChannel.getEpgUrl().isEmpty() && epgData.containsKey(jsonChannel.getEpgUrl())) {
            List<Program> programForGivenTime = new ArrayList<>();
            XmlTvParser.TvListing tvListing = epgData.get(jsonChannel.getEpgUrl());
            List<Program> programList = tvListing.getAllPrograms();
            // If repeat-programs is on, schedule the programs sequentially in a loop. To make
            // every device play the same program in a given channel and time, we assumes the
            // loop started from the epoch time.
            long totalDurationMs = 0;
            for (Program program : programList) {
                totalDurationMs += program.getEndTimeUtcMillis() - program.getStartTimeUtcMillis();
            }
            long programStartTimeMs = startMs - startMs % totalDurationMs;
            int i = 0;
            final int programCount = programList.size();
            while (programStartTimeMs < endMs) {
                Program currentProgram = programList.get(i++ % programCount);
                long programEndTimeMs = currentProgram.getEndTimeUtcMillis();
                if (programEndTimeMs < startMs) {
                    programStartTimeMs = programEndTimeMs;
                    continue;
                }
                programForGivenTime.add(new Program.Builder()
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
            programs.add(new Program.Builder()
                    .setInternalProviderData(channel.getInternalProviderData())
                    .setTitle(channel.getDisplayName() + " Live")
                    .setDescription(getString(R.string.currently_streaming))
                    .setPosterArtUri(channel.getChannelLogo())
                    .setThumbnailUri(channel.getChannelLogo())
                    .setCanonicalGenres(jsonChannel.getGenres())
                    .setStartTimeUtcMillis(startMs)
                    .setEndTimeUtcMillis(startMs + 1000 * 60 * 60) // 60 minutes
                    .build());
        }
        return programs;
    }

    private final class EpgDataSyncThread extends Thread {
        private Context mContext;
        private EpgDataCallback mCallback;

        EpgDataSyncThread(Context context, EpgDataCallback callback) {
            super();
            mContext = context;
            mCallback = callback;
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
                        } catch (IOException | XmlTvParser.XmlTvParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mCallback.onComplete();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private interface EpgDataCallback {
        void onComplete();
    }

    @Deprecated
    public static void requestImmediateSync1(Context context, String inputId, long syncDuration,
            ComponentName jobServiceComponent) {
        if (jobServiceComponent.getClass().isAssignableFrom(EpgSyncJobService.class)) {
            throw new IllegalArgumentException("This class does not extend EpgSyncJobService");
        }
        PersistableBundle persistableBundle = new PersistableBundle();
        if (Build.VERSION.SDK_INT >= 22) {
            persistableBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            persistableBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        }
        persistableBundle.putString(EpgSyncJobService.BUNDLE_KEY_INPUT_ID, inputId);
        persistableBundle.putLong("bundle_key_sync_period", syncDuration);
        JobInfo.Builder builder = new JobInfo.Builder(1, jobServiceComponent);
        JobInfo jobInfo = builder
                .setExtras(persistableBundle)
                .setOverrideDeadline(1000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();
        scheduleJob(context, jobInfo);
        Log.d(TAG, "Single job scheduled");
    }

    /** Send the job to JobScheduler. */
    private static void scheduleJob(Context context, JobInfo job) {
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int result = jobScheduler.schedule(job);
        Assert.assertEquals(result, JobScheduler.RESULT_SUCCESS);
        Log.d(TAG, "Scheduling result is " + result);
    }
}
