package com.felkertech.cumulustv.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.felkertech.cumulustv.fileio.AbstractFileParser;
import com.felkertech.cumulustv.fileio.FileParserFactory;
import com.felkertech.cumulustv.fileio.HttpFileParser;
import com.felkertech.cumulustv.fileio.M3uParser;
import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.cumulustv.plugins.JsonContainer;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.cumulustv.utils.DriveSettingsManager;
import com.felkertech.settingsmanager.SettingsManager;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
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
    private JSONArray mTemporaryObjects;
    private SettingsManager mSettingsManager;
    protected HashMap<String, Long> mDatabaseHashMap;

    private static ChannelDatabase mChannelDatabase;

    public static ChannelDatabase getInstance(Context context) {
        if (mChannelDatabase == null) {
            mChannelDatabase = new ChannelDatabase(context);
            mChannelDatabase.initializeHashMap(context);
            try {
                mChannelDatabase.readJsonListings();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
        final List<JsonChannel> channelList = new ArrayList<>();
        // Add all the normal channels
        for (int i = 0; i < channels.length(); i++) {
            ChannelDatabaseFactory.parseType(channels.getJSONObject(i), new ChannelDatabaseFactory.ChannelParser() {
                @Override
                public void ifJsonChannel(JsonChannel entry) {
                    channelList.add(entry);
                }

                @Override
                public void ifJsonListing(JsonListing entry) {
                }
            });
        }
        // Add temporary channels to list
        if (mTemporaryObjects != null) {
            for (int i = 0; i < mTemporaryObjects.length(); i++) {
                ChannelDatabaseFactory.parseType(mTemporaryObjects.getJSONObject(i), new ChannelDatabaseFactory.ChannelParser() {
                    @Override
                    public void ifJsonChannel(JsonChannel entry) {
                        channelList.add(entry);
                    }

                    @Override
                    public void ifJsonListing(JsonListing entry) {
                    }
                });
            }
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
            /*if (channel.getGenresString().isEmpty()) {
                channel = new CumulusChannel.Builder(channel)
                        .setGenres(TvContract.Programs.Genres.FAMILY_KIDS)
                        .build();
            }*/
            JSONArray channels = mJsonObject.getJSONArray(KEY_CHANNELS);
            channels.put(channel.toJson());
            save();
        }
    }

    public void add(JsonListing listing) throws JSONException {
        if (mJsonObject != null) {
            JSONArray channels = mJsonObject.getJSONArray(KEY_CHANNELS);
            channels.put(listing.toJson());
            save();
        }
    }

    public void update(CumulusChannel channel) throws JSONException {
        /*if (channel.getGenresString().isEmpty()) {
            channel = new CumulusChannel.Builder(channel)
                    .setGenres(TvContract.Programs.Genres.FAMILY_KIDS)
                    .build();
        }*/
        final CumulusChannel channel1 = channel;
        if(!channelExists(channel)) {
            add(channel);
        } else {
            final JSONArray channels = getJSONArray();
            final int[] i = new int[1];
            for (i[0] = 0; i[0] < channels.length(); i[0]++) {
                ChannelDatabaseFactory.parseType(channels.getJSONObject(i[0]), new ChannelDatabaseFactory.ChannelParser() {
                    @Override
                    public void ifJsonChannel(JsonChannel entry) {
                        try {
                            if (entry.getMediaUrl().equals(channel1.getMediaUrl())) {
                                if (DEBUG) {
                                    Log.d(TAG, "Remove " + i[0] + " and put at " + i[0] + ": " +
                                            channel1.toString());
                                }
                                channels.put(i[0], channel1.toJson());
                                mJsonObject.getJSONArray(KEY_CHANNELS).put(i[0], channel1.toJson());
                                save();
                                return;
                            }
                        } catch (JSONException ignored) {
                        }
                    }

                    @Override
                    public void ifJsonListing(JsonListing entry) {
                    }
                });
            }
        }
    }

    public void delete(final CumulusChannel channel) throws JSONException {
        final JSONArray channels = getJSONArray();
        final int[] i = new int[1];
        for (i[0] = 0; i[0] < channels.length(); i[0]++) {
            ChannelDatabaseFactory.parseType(channels.getJSONObject(i[0]), new ChannelDatabaseFactory.ChannelParser() {
                @Override
                public void ifJsonChannel(JsonChannel entry) {
                    try {
                        if (entry.getMediaUrl().equals(channel.getMediaUrl())) {
                            if (DEBUG) {
                                Log.d(TAG, "Remove " + i[0] + " and put at " + i[0] + ": " +
                                        channel.toString());
                            }
                            channels.put(i[0], channel.toJson());
                            mJsonObject.getJSONArray(KEY_CHANNELS).remove(i[0]);
                            save();
                            return;
                        }
                    } catch (JSONException ignored) {
                    }
                }

                @Override
                public void ifJsonListing(JsonListing entry) {
                }
            });
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
                                    cursor.getBlob(cursor.getColumnIndex(
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

    public void eraseData() {
        mSettingsManager.setString(ChannelDatabase.KEY, getDefaultJsonString());
        Log.d(TAG, "Erasing data");
        try {
            mJsonObject.put(KEY_CHANNELS, new JSONArray());
            Log.d(TAG, getJSONArray().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds temporary channels to the database, which live solely in memory and are wiped when the
     * app memory is deleted. These can interface with {@link JsonListing} objects to add channels
     * that are displayed but not saved.
     *
     * @param temp The channel to add.
     * @throws JSONException
     */
    public void addTemporaryChannel(JsonContainer temp) throws JSONException {
        if (mTemporaryObjects == null) {
            mTemporaryObjects = new JSONArray();
        }
        for (int i = 0; i < mTemporaryObjects.length(); i++) {
            if (mTemporaryObjects.get(i).equals(temp)) {
                return;
            }
        }
        mTemporaryObjects.put(temp.toJson());
    }

    /**
     * Scans through user data to find all json listings and add them to a temporary object in
     * memory.
     */
    protected void readJsonListings() throws JSONException {
        JSONArray jsonArray = getJSONArray();
        for (int i = 0; i < jsonArray.length(); i++) {
            ChannelDatabaseFactory.parseType(jsonArray.getJSONObject(i), new ChannelDatabaseFactory.ChannelParser() {
                @Override
                public void ifJsonChannel(JsonChannel entry) {
                }

                @Override
                public void ifJsonListing(JsonListing entry) {
                    Log.d(TAG, "Json listing " + entry.getUrl());
                    new HttpFileParser(entry.getUrl(), new AbstractFileParser.FileLoader() {
                        @Override
                        public void onFileLoaded(InputStream inputStream) {
                            try {
                                List<M3uParser.M3uTvChannel> channels =
                                        M3uParser.parse(inputStream).channels;
                                for (M3uParser.M3uTvChannel c : channels) {
                                    Log.d(TAG, "Reading " + c.url + " from JSON Listing");
                                    addTemporaryChannel(c.toJsonChannel());
                                }
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
        }
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
