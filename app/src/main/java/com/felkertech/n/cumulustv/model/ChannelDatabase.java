package com.felkertech.n.cumulustv.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.util.Log;
import android.widget.Toast;

import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.n.ActivityUtils;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.tv.activities.PlaybackQuickSettingsActivity;
import com.felkertech.settingsmanager.SettingsManager;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by N on 7/14/2015.
 * This is a JSON object that stores all relevant user-input data for channels
 */
public class ChannelDatabase {
    private static final String TAG = ChannelDatabase.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String KEY = "JSONDATA";

    private static final String KEY_CHANNELS = "channels";
    private static final String KEY_MODIFIED = "modified";

    protected JSONObject mJsonObject;
    private TvContentRating mTvContentRating;
    private SettingsManager mSettingsManager;
    private HashMap<String, Long> mDatabaseHashMap;

    private static ChannelDatabase mChannelDatabase;

    public static ChannelDatabase getInstance(Context context) {
        if (mChannelDatabase == null) {
            mChannelDatabase = new ChannelDatabase(context);
        }
        mChannelDatabase.initializeHashMap(context);
        return mChannelDatabase;
    }

    protected ChannelDatabase(final Context context) {
        try {
            DriveSettingsManager sp = new DriveSettingsManager(context);
            String spData = sp.getString(KEY, getDefaultJsonString());
            if (spData.isEmpty()) {
                spData = getDefaultJsonString();
            }
            mJsonObject = new JSONObject(spData);
            if (!mJsonObject.has("modified")) {
                mJsonObject.put("modified", 0L);
                save();
            }
            resetPossibleGenres(); // This will try to use the newest API data
        } catch (final JSONException e) {
            throw new RuntimeException(e.getMessage());
        }
        mTvContentRating = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D", "US_TV_L");
        mSettingsManager = new SettingsManager(context);
    }

    public JSONArray getJSONArray() throws JSONException {
        return mJsonObject.getJSONArray("channels");
    }

    public ArrayList<JsonChannel> getJsonChannels() throws JSONException {
        JSONArray channels = getJSONArray();
        ArrayList<JsonChannel> channelList = new ArrayList<>();
        for (int i = 0; i < channels.length(); i++) {
            JsonChannel channel = new JsonChannel.Builder(channels.getJSONObject(i)).build();
            channelList.add(channel);
        }
        return channelList;
    }

    public ArrayList<Channel> getChannels() throws JSONException {
        ArrayList<JsonChannel> jsonChannelList = getJsonChannels();
        ArrayList<Channel> channelList = new ArrayList<>();
        for (int i = 0; i < jsonChannelList.size(); i++) {
            JsonChannel jsonChannel = jsonChannelList.get(i);
            Channel channel = jsonChannel.toChannel();
            channel.setAppLinkColor("#9C27B0");
            channel.setAppLinkPoster(jsonChannel.getLogo());
            channel.setAppLinkText(mSettingsManager.getContext().getString(R.string.quick_settings));
            channel.setAppLinkIntent(PlaybackQuickSettingsActivity
                    .getIntent(mSettingsManager.getContext(),
                            jsonChannel.toString()));
            channelList.add(channel);
        }
        return channelList;
    }

    public boolean channelNumberExists(String number) {
        try {
            ArrayList<JsonChannel> jsonChannelList = getJsonChannels();
            for (JsonChannel jsonChannel : jsonChannelList) {
                if (jsonChannel.getNumber().equals(number)) {
                    return true;
                }
            }
        } catch (JSONException ignored) {
        }
        return false;
    }

    public boolean channelExists(JsonChannel channel) {
        try {
            ArrayList<JsonChannel> jsonChannelList = getJsonChannels();
            for (JsonChannel jsonChannel : jsonChannelList) {
                if (jsonChannel.equals(channel)) {
                    return true;
                }
            }
        } catch (JSONException ignored) {
        }
        return false;
    }

    public JsonChannel findChannelByChannelNumber(String channelNumber) {
        try {
            ArrayList<JsonChannel> jsonChannelList = getJsonChannels();
            for (JsonChannel jsonChannel : jsonChannelList) {
                if (jsonChannel.getNumber() != null) {
                    if (jsonChannel.getNumber().equals(channelNumber)) {
                        return jsonChannel;
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    public JsonChannel findChannelByMediaUrl(String mediaUrl) {
        try {
            ArrayList<JsonChannel> jsonChannelList = getJsonChannels();
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
        ArrayList<String> strings = new ArrayList<>();
        try {
            ArrayList<JsonChannel> jsonChannelList = getJsonChannels();
            for (JsonChannel jsonChannel : jsonChannelList) {
                strings.add(jsonChannel.getNumber() + " " + jsonChannel.getName());
            }
        } catch (JSONException ignored) {
        }
        return strings.toArray(new String[strings.size()]);
    }

    public void add(JsonChannel channel) throws JSONException {
        if (mJsonObject != null) {
            JSONArray channels = mJsonObject.getJSONArray("channels");
            channels.put(channel.toJSON());
            save();
        }
    }

    public void update(JsonChannel channel) throws JSONException {
        if(!channelExists(channel)) {
            add(channel);
        } else {
            try {
                JSONArray jsonArray = new JSONArray();
                ArrayList<JsonChannel> jsonChannelList = getJsonChannels();
                int finalindex = -1;
                for (int i = 0; i < jsonChannelList.size(); i++) {
                    JsonChannel jsonChannel = jsonChannelList.get(i);
                    if (finalindex >= 0) {
//                        jsonArray.put(finalindex, ch.toJSON());
                    } else if(jsonChannel.getMediaUrl().equals(channel.getMediaUrl())) {
                        if (DEBUG) {
                            Log.d(TAG, "Remove " + i + " and put at " + i + ": " +
                                    channel.toJSON().toString());
                        }
                        jsonArray.put(i, channel.toJSON());
                        finalindex = i;
                        save();
                    }
                }
            } catch (JSONException ignored) {
            }
        }
    }

    public void delete(JsonChannel channel) throws JSONException {
        if(!channelExists(channel)) {
            add(channel);
        } else {
            try {
                ArrayList<JsonChannel> jsonChannelList = getJsonChannels();
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

    public void resetPossibleGenres() throws JSONException {
        JSONArray genres = new JSONArray();
        genres.put(TvContract.Programs.Genres.ANIMAL_WILDLIFE);
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
    private void initializeHashMap(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentResolver contentResolver = context.getContentResolver();
                Uri channelsUri = TvContract.buildChannelsUriForInput(
                        ActivityUtils.TV_INPUT_SERVICE.flattenToString());
                Cursor cursor = contentResolver.query(channelsUri, null, null, null, null);
                mDatabaseHashMap = new HashMap<>();
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String mediaUrl = cursor.getString(cursor.getColumnIndex(
                                TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA));
                        long rowId = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID));
                        try {
                            for (JsonChannel jsonChannel : getJsonChannels()) {
                                if (jsonChannel.getMediaUrl().equals(mediaUrl)) {
                                    mDatabaseHashMap.put(jsonChannel.getMediaUrl(), rowId);
                                }
                            }
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

    public static int getAvailableChannelNumber(Context mContext) {
        ChannelDatabase cd = new ChannelDatabase(mContext);
        int i = 1;
        while (cd.channelNumberExists(String.valueOf(i))) {
            i++;
        }
        return i;
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
}
