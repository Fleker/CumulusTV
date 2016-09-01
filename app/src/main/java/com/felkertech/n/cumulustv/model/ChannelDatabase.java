package com.felkertech.n.cumulustv.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.cumulustv.R;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by N on 7/14/2015.
 * This is a JSON object that stores all relevant user-input data for channels
 */
public class ChannelDatabase {
    private static final String TAG = ChannelDatabase.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String KEY = "JSONDATA";

    private JSONObject obj;
    private TvContentRating rating;
    private Context mContext;

    private static ChannelDatabase mChannelDatabase;

    public static ChannelDatabase getInstance(Context context) {
        if (mChannelDatabase == null) {
            mChannelDatabase = new ChannelDatabase(context);
        }
        return mChannelDatabase;
    }

    private ChannelDatabase(final Context context) {
        try {
            DriveSettingsManager sp = new DriveSettingsManager(context);
            String spData = sp.getString(KEY, "{'channels':[], 'modified':0}");
            obj = new JSONObject(spData);
            if(!obj.has("modified")) {
                obj.put("modified", 0l);
                save();
            }
            resetPossibleGenres(); //This will try to use the newest API data
        } catch (final JSONException e) {
            Handler h = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    Toast.makeText(context, context.getString(R.string.toast_please_report,
                            e.getMessage()), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            };
            h.sendEmptyMessage(0);
        }
        rating = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D", "US_TV_L");
    }

    public JSONArray getJSONChannels() throws JSONException {
        Assert.assertNotNull(mContext.getString(R.string.toast_report_database_null), obj);
        return obj.getJSONArray("channels");
    }
    public ArrayList<Channel> getChannels() throws JSONException {
        JSONArray channels = getJSONChannels();
        ArrayList<Channel> channelInfos = new ArrayList<>();
        for(int i = 0; i<channels.length();i++) {
            JSONChannel channel = new JSONChannel(channels.getJSONObject(i));
            Channel ci = new Channel();
            ci.setNumber(channel.getNumber());
            ci.setName(channel.getName());
            if(ci.getName() != null) {
                ci.setOriginalNetworkId(ci.getName().hashCode());
            }
            ci.setTransportStreamId(1);
            ci.setServiceId(i+2);
            ci.setVideoHeight(1080);
            ci.setVideoWidth(1920);
            ci.setLogoUrl(channel.getLogo());
            Log.d(TAG, "Channel getUrl in GC = "+channel.getUrl());
            ci.setInternalProviderData(channel.getUrl());
            channelInfos.add(ci);
        }
        return channelInfos;
    }

    public boolean channelNumberExists(String number) {
        try {
            JSONArray jsonArray = getJSONChannels();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
                if (jsonChannel.getNumber().equals(number)) {
                    return true;
                }
            }
        } catch (JSONException ignored) {
        }
        return false;
    }

    public boolean channelExists(JSONChannel jsonChannel) {
        try {
            JSONArray jsonArray = getJSONChannels();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONChannel channel = new JSONChannel(jsonArray.getJSONObject(i));
                if (channel.getNumber().equals(jsonChannel.getNumber()) ||
                        channel.getName().equals(jsonChannel.getName()) ||
                        channel.getUrl().equals(jsonChannel.getUrl())) {
                    return true;
                }
            }
        } catch (JSONException ignored) {
        }
        return false;
    }

    public JSONChannel findChannel(String channelNumber) {
        try {
            JSONArray jsonArray = getJSONChannels();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
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

    public String[] getChannelNames() {
        ArrayList<String> strings = new ArrayList<>();
        try {
            JSONArray jsonArray = getJSONChannels();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
                strings.add(jsonChannel.getNumber() + " " + jsonChannel.getName());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            try {
                obj = new JSONObject("{'channels':[]}");
                save();
                //FIXME Seems a bit harsh
                Log.d(TAG, "A problem occurred; resetting all your data");
                return getChannelNames();
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
        return strings.toArray(new String[strings.size()]);
    }

    public void add(JSONChannel n00b) throws JSONException {
        if (obj != null) {
            JSONArray channels = obj.getJSONArray("channels");
            channels.put(n00b.toJSON());
            save();
        } else {
            Handler toasty = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    Toast.makeText(mContext, R.string.toast_error_object_undefined,
                            Toast.LENGTH_SHORT).show();
                }
            };
            toasty.sendEmptyMessage(0);
        }
    }

    public void update(JSONChannel ch) throws JSONException {
        if(!channelExists(ch)) {
            add(ch);
        } else {
            try {
                JSONArray jsonArray = getJSONChannels();
                int finalindex = -1;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
                    if (finalindex >= 0) {
//                        jsonArray.put(finalindex, ch.toJSON());
                    } else if(jsonChannel.getUrl().equals(ch.getUrl())) {
                        if (DEBUG) {
                            Log.d(TAG, "Remove " + i + " and put at " + i + ": " +
                                    ch.toJSON().toString());
                        }
                        jsonArray.put(i, ch.toJSON());
                        finalindex = i;
                        save();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void update(JSONChannel ch, int index) throws JSONException {
        //With provided index, we can easily update that channel!
        if(!channelExists(ch)) {
            add(ch);
            return;
        }
        JSONArray jsonArray = getJSONChannels();
        jsonArray.remove(index);
        jsonArray.put(index, ch.toJSON());
        save();
    }

    public void delete(JSONChannel n00b) throws JSONException {
        if(!channelExists(n00b)) {
            add(n00b);
        } else {
            try {
                JSONArray jsonArray = getJSONChannels();
                for(int i = 0;i<jsonArray.length();i++) {
                    JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
                    if(jsonChannel.getUrl() != null && jsonChannel.getUrl().equals(n00b.getUrl())) {
                        jsonArray.remove(i);
                        save();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        try {
            setLastModified();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor e = sp.edit();
            e.putString(KEY, toString());
            e.commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        Assert.assertNotNull(mContext.getString(R.string.toast_report_database_null), obj);
        return obj.toString();
    }

    public long getLastModified() throws JSONException {
        Assert.assertNotNull(mContext.getString(R.string.toast_report_database_null), obj);
        return obj.getLong("modified");
    }

    public void setLastModified() throws JSONException {
        if(obj != null) {
            obj.put("modified", System.currentTimeMillis());
        }
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
        obj.put("possibleGenres", genres);
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
}
