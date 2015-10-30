package com.felkertech.n.cumulustv;

import android.bluetooth.le.AdvertiseData;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.android.sampletvinput.player.TvInputPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by N on 7/14/2015.
 * This is a JSON object that stores all relevant user-input data for channels
 */
public class ChannelDatabase {
    JSONObject obj;
    TvContentRating rating;
    Context mContext;
    String TAG = "cumulus:ChannelDatabase";
    public static final String KEY = "JSONDATA";
    public ChannelDatabase(Context mContext) {
        this.mContext = mContext;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        String spData = sp.getString(KEY, "{'channels':[], 'modified':0}");
        try {
            obj = new JSONObject(spData);
            if(!obj.has("modified")) {
                obj.put("modified", 0l);
                save();
            }
            resetPossibleGenres(); //This will try to use the newest API data
        } catch (JSONException e) {
            Toast.makeText(mContext, "Please report this error with your JSON file: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        rating = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D", "US_TV_L");
    }
    public JSONArray getJSONChannels() throws JSONException {
        if(obj != null) {
            JSONArray channels = obj.getJSONArray("channels");
            return channels;
        } else {
            Toast.makeText(mContext, "Report this error with your JSON file: DatabaseObject2 is null", Toast.LENGTH_SHORT).show();
        }
        return new JSONArray();
    }
    public ArrayList<TvManager.ChannelInfo> getChannels() throws JSONException {
        JSONArray channels = getJSONChannels();
        ArrayList<TvManager.ChannelInfo> channelInfos = new ArrayList<>();
        for(int i = 0; i<channels.length();i++) {
            JSONChannel channel = new JSONChannel(channels.getJSONObject(i));
            TvManager.ChannelInfo ci = new TvManager.ChannelInfo();
            ci.number = channel.getNumber();
            ci.name = channel.getName();
            if(ci.name != null)
                ci.originalNetworkId = ci.name.hashCode();
//            Log.d(TAG, "Hash "+ci.originalNetworkId+" for "+ci.name);
//            ci.originalNetworkId = i+1;
            ci.transportStreamId = 1;
            ci.serviceId = i+2;
//            ci.serviceId = 2;
            ci.videoHeight = 1080;
            ci.videoWidth = 1920;
            ci.logoUrl = channel.getLogo();
            ci.programs = getPrograms(ci, channel.getUrl());
            channelInfos.add(ci);
        }
        return channelInfos;
    }
    public List<TvManager.ProgramInfo> getPrograms(TvManager.ChannelInfo channelInfo, String streamUrl) {
        List<TvManager.ProgramInfo> infoList = new ArrayList<>();
//        String streamUrl = channelInfo.programs.get(0).videoUrl;
        JSONChannel jsonChannel = findChannel(channelInfo.number);
        infoList.add(new TvManager.ProgramInfo(channelInfo.name+" Live", channelInfo.logoUrl,
                "Currently streaming", 60*60, new TvContentRating[] {rating}, jsonChannel.getGenres(), streamUrl, TvInputPlayer.SOURCE_TYPE_HTTP_PROGRESSIVE, 0));
        return infoList;
    }
    public List<TvManager.ProgramInfo> getPrograms(JSONChannel jsonChannel) {
        List<TvManager.ProgramInfo> infoList = new ArrayList<>();
        String streamUrl = jsonChannel.getUrl();
        infoList.add(new TvManager.ProgramInfo(jsonChannel.getName()+" Live", jsonChannel.getLogo(),
                "Currently streaming", 60*60, new TvContentRating[] {rating}, jsonChannel.getGenres(), streamUrl, TvInputPlayer.SOURCE_TYPE_HTTP_PROGRESSIVE, 0));
        return infoList;
    }
    public boolean channelNumberExists(String number) {
        try {
            JSONArray jsonArray = getJSONChannels();
            for(int i = 0;i<jsonArray.length();i++) {
                JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
                if(jsonChannel.getNumber().equals(number))
                    return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean channelExists(JSONChannel jsonChannel) {
        try {
            JSONArray jsonArray = getJSONChannels();
            for(int i = 0;i<jsonArray.length();i++) {
                JSONChannel channel = new JSONChannel(jsonArray.getJSONObject(i));
                if(channel.getNumber().equals(jsonChannel.getNumber()))
                    return true;
                if(channel.getName().equals(jsonChannel.getName()))
                    return true;
                if(channel.getUrl().equals(jsonChannel.getUrl()))
                    return true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
    public JSONChannel findChannel(String channelNumber) {
        try {
            JSONArray jsonArray = getJSONChannels();
            for(int i = 0;i<jsonArray.length();i++) {
                JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
                if(jsonChannel.getNumber() != null) {
                    if (jsonChannel.getNumber().equals(channelNumber))
                        return jsonChannel;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String[] getChannelNames() {
        ArrayList<String> strings = new ArrayList<>();
        try {
            JSONArray jsonArray = getJSONChannels();
            for(int i = 0;i<jsonArray.length();i++) {
                JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
                strings.add(jsonChannel.getNumber()+" "+jsonChannel.getName());
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
        if(obj != null) {
            JSONArray channels = obj.getJSONArray("channels");
            channels.put(n00b.toJSON());
            save();
        } else {
            Toast.makeText(mContext, "Error adding: object is undefined", Toast.LENGTH_SHORT).show();
        }

    }
    public void update(JSONChannel ch) throws JSONException {
        if(!channelExists(ch)) {
            add(ch);
            return;
        } else {
            try {
                JSONArray jsonArray = getJSONChannels();
                int finalindex = -1;
                for(int i = 0;i<jsonArray.length();i++) {
                    JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
                    if(finalindex >= 0) {
//                        jsonArray.put(finalindex, ch.toJSON());
                    } else if(jsonChannel.getUrl().equals(ch.getUrl())) {
                        Log.d(TAG, "Remove "+i+" and put at "+i+": "+ch.toJSON().toString());
                        jsonArray.put(i, ch.toJSON());
//                        jsonArray.remove(i);
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
            return;
        } else {
            try {
                JSONArray jsonArray = getJSONChannels();
                for(int i = 0;i<jsonArray.length();i++) {
                    JSONChannel jsonChannel = new JSONChannel(jsonArray.getJSONObject(i));
                    if(jsonChannel.getUrl().equals(n00b.getUrl())) {
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
        if(obj != null) {
            return obj.toString();
        } else {
            Toast.makeText(mContext, "Report this error with your JSON file: DatabaseObject is null", Toast.LENGTH_SHORT).show();
        }
        return "";
    }
    public long getLastModified() throws JSONException {
        if(obj != null) {
            return obj.getLong("modified");
        } else {
            Toast.makeText(mContext, "Report this error with your JSON file: DatabaseObject4 is null", Toast.LENGTH_SHORT).show();
            return -1;
        }
    }
    public void setLastModified() throws JSONException {
        obj.put("modified", new Date().getTime());
    }

    public void resetPossibleGenres() throws JSONException {
        JSONArray genres = new JSONArray();
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
        obj.put("possibleGenres", genres);
    }

    public static String[] getAllGenres() {
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
}
