package com.felkertech.cumulustv.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.felkertech.cumulustv.fileio.M3uParser;
import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.cumulustv.utils.DriveSettingsManager;
import com.felkertech.settingsmanager.SettingsManager;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by N on 7/14/2015.
 * This is a JSON object that stores all relevant user-input data for channels
 */
public class ChannelDatabase {
    private static final String TAG = ChannelDatabase.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final String KEY = "JSONDATA";
    private static final String KEY_CHANNELS = "channels";
    private static final String KEY_MODIFIED = "modified";

    private JSONObject mJsonObject;
    private SettingsManager mSettingsManager;
    protected HashMap<String, Long> mDatabaseHashMap;

    private static ChannelDatabase mChannelDatabase;

    public static ChannelDatabase getInstance(Context context) {
        if (mChannelDatabase == null) {
            mChannelDatabase = new ChannelDatabase(context);
        }
        mChannelDatabase.initializeHashMap(context);
        return mChannelDatabase;
    }

    protected ChannelDatabase(final Context context) {
        mSettingsManager = new SettingsManager(context);
        try {
            DriveSettingsManager sp = new DriveSettingsManager(context);
            String spData = sp.getString(KEY, getDefaultJsonString());
            if (spData.isEmpty()) {
                spData = getDefaultJsonString();
            }
            mJsonObject = new JSONObject(spData);
            if (!mJsonObject.has(KEY_MODIFIED)) {
                mJsonObject.put(KEY_MODIFIED, 0L);
                save();
            }
            resetPossibleGenres(); // This will try to use the newest API data
        } catch (final JSONException e) {
            throw new MalformedChannelDataException(e.getMessage());
        }
    }

    public JSONArray getJSONArray() throws JSONException {
        return mJsonObject.getJSONArray(KEY_CHANNELS);
    }

    public List<JsonChannel> getJsonChannels() throws JSONException {
        JSONArray channels = getJSONArray();
        List<JsonChannel> channelList = new ArrayList<>();
        for (int i = 0; i < channels.length(); i++) {
            JsonChannel channel = new JsonChannel.Builder(channels.getJSONObject(i)).build();
            channelList.add(channel);
        }
        return channelList;
    }

    public List<Channel> getChannels() throws JSONException {
        List<JsonChannel> jsonChannelList = getJsonChannels();
        List<Channel> channelList = new ArrayList<>();
        for (int i = 0; i < jsonChannelList.size(); i++) {
            JsonChannel jsonChannel = jsonChannelList.get(i);
            Channel channel = jsonChannel.toChannel();
            channelList.add(channel);
        }
        return channelList;
    }

    public List<Channel> getChannels(InternalProviderData providerData) throws JSONException {
        List<JsonChannel> jsonChannelList = getJsonChannels();
        List<Channel> channelList = new ArrayList<>();
        for (int i = 0; i < jsonChannelList.size(); i++) {
            JsonChannel jsonChannel = jsonChannelList.get(i);
            Channel channel = jsonChannel.toChannel(providerData);
            channelList.add(channel);
        }
        return channelList;
    }

    public boolean channelNumberExists(String number) {
        try {
            List<JsonChannel> jsonChannelList = getJsonChannels();
            for (JsonChannel jsonChannel : jsonChannelList) {
                if (jsonChannel.getNumber().equals(number)) {
                    return true;
                }
            }
        } catch (JSONException ignored) {
        }
        return false;
    }

    public boolean channelExists(CumulusChannel channel) {
        try {
            List<JsonChannel> jsonChannelList = getJsonChannels();
            for (JsonChannel jsonChannel : jsonChannelList) {
                if (jsonChannel.equals(channel) ||
                        jsonChannel.getMediaUrl().equals(channel.getMediaUrl())) {
                    return true;
                }
            }
        } catch (JSONException ignored) {
        }
        return false;
    }

    public JsonChannel findChannelByMediaUrl(String mediaUrl) {
        try {
            List<JsonChannel> jsonChannelList = getJsonChannels();
            for (JsonChannel jsonChannel : jsonChannelList) {
                if (jsonChannel.getMediaUrl() != null) {
                    if (jsonChannel.getMediaUrl().equals(mediaUrl)) {
                        return jsonChannel;
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    public String[] getChannelNames() {
        List<String> strings = new ArrayList<>();
        try {
            List<JsonChannel> jsonChannelList = getJsonChannels();
            for (JsonChannel jsonChannel : jsonChannelList) {
                strings.add(jsonChannel.getNumber() + " " + jsonChannel.getName());
            }
        } catch (JSONException ignored) {
        }
        return strings.toArray(new String[strings.size()]);
    }

    public void add(CumulusChannel channel) throws JSONException {
        if (mJsonObject != null) {
            JSONArray channels = mJsonObject.getJSONArray(KEY_CHANNELS);
            channels.put(channel.toJSON());
            save();
        }
    }

    public void update(CumulusChannel channel) throws JSONException {
        if(!channelExists(channel)) {
            add(channel);
        } else {
            try {
                JSONArray jsonArray = new JSONArray();
                List<JsonChannel> jsonChannelList = getJsonChannels();
                int finalindex = -1;
                for (int i = 0; i < jsonChannelList.size(); i++) {
                    JsonChannel jsonChannel = jsonChannelList.get(i);
                    if (finalindex >= 0) {
//                        jsonArray.put(finalindex, ch.toJSON());
                    } else if(jsonChannel.getMediaUrl().equals(channel.getMediaUrl())) {
                        if (DEBUG) {
                            Log.d(TAG, "Remove " + i + " and put at " + i + ": " +
                                    channel.toString());
                        }
                        jsonArray.put(i, channel.toJSON());
                        finalindex = i;
                        mJsonObject.getJSONArray(KEY_CHANNELS).put(i, channel.toJSON());
                        save();
                    }
                }
            } catch (JSONException ignored) {
            }
        }
    }

    public void delete(CumulusChannel channel) throws JSONException {
        if(!channelExists(channel)) {
            add(channel);
        } else {
            try {
                List<JsonChannel> jsonChannelList = getJsonChannels();
                for (int i = 0; i < jsonChannelList.size(); i++) {
                    JsonChannel jsonChannel = jsonChannelList.get(i);
                    if(jsonChannel.getMediaUrl() != null &&
                            jsonChannel.getMediaUrl().equals(channel.getMediaUrl())) {
                        mJsonObject.getJSONArray(KEY_CHANNELS).remove(i);
                        save();
                    }
                }
            } catch (JSONException ignored) {
            }
        }
    }

    public void save() {
        try {
            setLastModified();
            mSettingsManager.setString(KEY, toString());
            initializeHashMap(mSettingsManager.getContext());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return mJsonObject.toString();
    }

    public String toM3u() {
        StringBuilder builder = new StringBuilder();
        builder.append(M3uParser.Constants.HEADER_TAG + "\n");
        try {
            List<JsonChannel> jsonChannelList = getJsonChannels();
            for (int i = 0; i< jsonChannelList.size(); i++) {
                JsonChannel jsonChannel = jsonChannelList.get(i);
                builder.append(M3uParser.Constants.CHANNEL_TAG);
                builder.append(" " + M3uParser.Constants.CH_NUMBER + "=\"")
                        .append(jsonChannel.getNumber()).append("\"");
                if (jsonChannel.hasLogo()) {
                    builder.append(" " + M3uParser.Constants.CH_LOGO + "=\"")
                            .append(jsonChannel.getLogo()).append("\"");
                }
                if (jsonChannel.isAudioOnly()) {
                    builder.append(" " + M3uParser.Constants.CH_AUDIO_ONLY + "=1");
                }
                if (jsonChannel.getEpgUrl() != null) {
                    builder.append(" " + M3uParser.Constants.CH_EPG_URL + "=\"")
                            .append(jsonChannel.getEpgUrl()).append("\"");
                }
                builder.append(" " + M3uParser.Constants.CH_GENRES + "=\"")
                        .append(jsonChannel.getGenresString()).append("\"");
                if (jsonChannel.getPluginSource() != null) {
                    builder.append(" " + M3uParser.Constants.CH_PLUGIN + "=\"")
                            .append(jsonChannel.getPluginSource()).append("\"");
                }
                if (jsonChannel.hasSplashscreen()) {
                    builder.append(" " + M3uParser.Constants.CH_SPLASH + "=\"")
                            .append(jsonChannel.getSplashscreen()).append("\"");
                }
                builder.append(", ").append(jsonChannel.getName()).append("\n");
                // Add URL
                builder.append(jsonChannel.getMediaUrl()).append("\n");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public long getLastModified() throws JSONException {
        return mJsonObject.getLong("modified");
    }

    private void setLastModified() throws JSONException {
        if(mJsonObject != null) {
            mJsonObject.put("modified", System.currentTimeMillis());
        }
    }

    public HashMap<String, Long> getHashMap() {
        return mDatabaseHashMap;
    }

    public JsonChannel getChannelFromRowId(@NonNull Long rowId) {
        if (mDatabaseHashMap == null || rowId < 0) {
            return null;
        }
        Set<String> mediaUrlSet = mDatabaseHashMap.keySet();
        for (String mediaUrl : mediaUrlSet) {
            if (mDatabaseHashMap.get(mediaUrl).equals(rowId)) {
                return findChannelByMediaUrl(mediaUrl);
            }
        }
        return null;
    }

    public void resetPossibleGenres() throws JSONException {
        JSONArray genres = new JSONArray();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            genres.put(TvContract.Programs.Genres.ANIMAL_WILDLIFE);
            genres.put(TvContract.Programs.Genres.ARTS);
            genres.put(TvContract.Programs.Genres.COMEDY);
            genres.put(TvContract.Programs.Genres.DRAMA);
            genres.put(TvContract.Programs.Genres.EDUCATION);
            genres.put(TvContract.Programs.Genres.ENTERTAINMENT);
            genres.put(TvContract.Programs.Genres.FAMILY_KIDS);
            genres.put(TvContract.Programs.Genres.GAMING);
            genres.put(TvContract.Programs.Genres.LIFE_STYLE);
            genres.put(TvContract.Programs.Genres.MOVIES);
            genres.put(TvContract.Programs.Genres.MUSIC);
            genres.put(TvContract.Programs.Genres.NEWS);
            genres.put(TvContract.Programs.Genres.PREMIER);
            genres.put(TvContract.Programs.Genres.SHOPPING);
            genres.put(TvContract.Programs.Genres.SPORTS);
            genres.put(TvContract.Programs.Genres.TECH_SCIENCE);
            genres.put(TvContract.Programs.Genres.TRAVEL);
        } else {
            genres.put(TvContract.Programs.Genres.ANIMAL_WILDLIFE);
            genres.put(TvContract.Programs.Genres.COMEDY);
            genres.put(TvContract.Programs.Genres.DRAMA);
            genres.put(TvContract.Programs.Genres.EDUCATION);
            genres.put(TvContract.Programs.Genres.FAMILY_KIDS);
            genres.put(TvContract.Programs.Genres.GAMING);
            genres.put(TvContract.Programs.Genres.MOVIES);
            genres.put(TvContract.Programs.Genres.NEWS);
            genres.put(TvContract.Programs.Genres.SHOPPING);
            genres.put(TvContract.Programs.Genres.SPORTS);
            genres.put(TvContract.Programs.Genres.TRAVEL);
        }
        mJsonObject.put("possibleGenres", genres);
    }

    /**
     * Creates a link between the database Uris and the JSONChannels
     * @param context The application's context for the {@link ContentResolver}.
     */
    protected void initializeHashMap(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentResolver contentResolver = context.getContentResolver();
                Uri channelsUri = TvContract.buildChannelsUriForInput(
                        ActivityUtils.TV_INPUT_SERVICE.flattenToString());
                Cursor cursor = contentResolver.query(channelsUri, null, null, null, null);
                mDatabaseHashMap = new HashMap<>();
                Log.d(TAG, "Initialize CD HashMap");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        try {
                            InternalProviderData ipd = new InternalProviderData(
                                    cursor.getString(cursor.getColumnIndex(
                                    TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA)));
                            String mediaUrl = ipd.getVideoUrl();
                            long rowId = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID));
                            Log.d(TAG, "Try to match " + mediaUrl + " " + rowId);
                            for (JsonChannel jsonChannel : getJsonChannels()) {
                                if (jsonChannel.getMediaUrl().equals(mediaUrl)) {
                                    mDatabaseHashMap.put(jsonChannel.getMediaUrl(), rowId);
                                }
                            }
                        } catch (InternalProviderData.ParseException e) {
                            e.printStackTrace();
                        } catch (JSONException ignored) {
                        }
                    }
                    cursor.close();
                }
            }
        }).start();
    }

    public static String[] getAllGenres() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return new String[] {
                    TvContract.Programs.Genres.ANIMAL_WILDLIFE,
                    TvContract.Programs.Genres.ARTS,
                    TvContract.Programs.Genres.COMEDY,
                    TvContract.Programs.Genres.DRAMA,
                    TvContract.Programs.Genres.EDUCATION,
                    TvContract.Programs.Genres.ENTERTAINMENT,
                    TvContract.Programs.Genres.FAMILY_KIDS,
                    TvContract.Programs.Genres.GAMING,
                    TvContract.Programs.Genres.LIFE_STYLE,
                    TvContract.Programs.Genres.MOVIES,
                    TvContract.Programs.Genres.MUSIC,
                    TvContract.Programs.Genres.NEWS,
                    TvContract.Programs.Genres.PREMIER,
                    TvContract.Programs.Genres.SHOPPING,
                    TvContract.Programs.Genres.SPORTS,
                    TvContract.Programs.Genres.TECH_SCIENCE,
                    TvContract.Programs.Genres.TRAVEL,
            };
        }
        return new String[] {
            TvContract.Programs.Genres.ANIMAL_WILDLIFE,
            TvContract.Programs.Genres.COMEDY,
            TvContract.Programs.Genres.DRAMA,
            TvContract.Programs.Genres.EDUCATION,
            TvContract.Programs.Genres.FAMILY_KIDS,
            TvContract.Programs.Genres.GAMING,
            TvContract.Programs.Genres.MOVIES,
            TvContract.Programs.Genres.NEWS,
            TvContract.Programs.Genres.SHOPPING,
            TvContract.Programs.Genres.SPORTS,
            TvContract.Programs.Genres.TRAVEL,
        };
    }

    protected static String getDefaultJsonString() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(KEY_CHANNELS, new JSONArray());
            jsonObject.put(KEY_MODIFIED, 0);
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Default JSON String cannot be created");
    }

    public static String getNonNullChannelLogo(CumulusChannel jsonChannel) {
        if (jsonChannel.hasLogo()) {
            return jsonChannel.getLogo();
        }
        return "https://raw.githubusercontent.com/Fleker/CumulusTV/master/app/src/main/res/drawab" +
                "le-xhdpi/c_banner_3_2.jpg";
    }

    public static class MalformedChannelDataException extends RuntimeException {
        public MalformedChannelDataException(String reason) {
            super(reason);
        }
    }
}
