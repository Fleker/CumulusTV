package com.felkertech.cumulustv.services;

import android.net.Uri;
import android.util.Log;

import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.tv.activities.PlaybackQuickSettingsActivity;
import com.felkertech.n.cumulustv.R;
import com.google.android.media.tv.companionlibrary.EpgSyncJobService;
import com.google.android.media.tv.companionlibrary.model.Advertisement;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * A periodic task that can be run to synchronize data from Google Drive to the system's internal
 * TIF database and local cache.
 */
public class CumulusJobService extends EpgSyncJobService {
    private static final String TAG = CumulusJobService.class.getSimpleName();
    private static String TEST_AD_REQUEST_URL =
            "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/" +
                    "single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast" +
                    "&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct" +
                    "%3Dlinear&correlator=";

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
        ipd.setAds(channelAdList);
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
                channels.add(i, channel);
            }
            return channels;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Program> getProgramsForChannel(Uri channelUri, Channel channel, long startMs,
           long endMs) {
        List<Program> programs = new ArrayList();
        ChannelDatabase channelDatabase = ChannelDatabase.getInstance(this);
        JsonChannel jsonChannel = channelDatabase.findChannelByMediaUrl(
                channel.getInternalProviderData().getVideoUrl());
        programs.add(new Program.Builder()
                .setInternalProviderData(channel.getInternalProviderData())
                .setTitle(channel.getDisplayName() + " Live")
                .setDescription("Currently streaming")
                .setPosterArtUri(channel.getChannelLogo())
                .setThumbnailUri(channel.getChannelLogo())
                .setCanonicalGenres(jsonChannel.getGenres())
                .setStartTimeUtcMillis(startMs)
                .setEndTimeUtcMillis(startMs + 1000 * 60 * 60) // 60 minutes
                .build());
        return programs;
    }
}
