package com.felkertech.n.cumulustv;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.android.sampletvinput.player.TvInputPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
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
        String spData = sp.getString(KEY, "{'channels':[]}");
        try {
            obj = new JSONObject(spData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        rating = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D", "US_TV_L");
    }
    public JSONArray getJSONChannels() throws JSONException {
        JSONArray channels = obj.getJSONArray("channels");
        return channels;
    }
    public ArrayList<TvManager.ChannelInfo> getChannels() throws JSONException {
        JSONArray channels = getJSONChannels();
        ArrayList<TvManager.ChannelInfo> channelInfos = new ArrayList<>();
        for(int i = 0; i<channels.length();i++) {
            JSONChannel channel = new JSONChannel(channels.getJSONObject(i));
            TvManager.ChannelInfo ci = new TvManager.ChannelInfo();
            ci.number = channel.getNumber();
            ci.name = channel.getName();
            ci.originalNetworkId = ci.name.toString().hashCode();;
            Log.d(TAG, "Hash "+ci.originalNetworkId+" for "+ci.name);
//            ci.originalNetworkId = i+1;
            ci.transportStreamId = 1;
            ci.serviceId = i+2;
//            ci.serviceId = 2;
            ci.videoHeight = 1080;
            ci.videoWidth = 1920;
//            ci.logoUrl = channel.getLogo();
            ci.programs = getPrograms(ci, channel.getUrl());
            channelInfos.add(ci);
        }
        return channelInfos;
    }
    public List<TvManager.ProgramInfo> getPrograms(TvManager.ChannelInfo channelInfo, String streamUrl) {
        List<TvManager.ProgramInfo> infoList = new ArrayList<>();
//        String streamUrl = channelInfo.programs.get(0).videoUrl;
        infoList.add(new TvManager.ProgramInfo(channelInfo.name+" Live", channelInfo.logoUrl,
                "Currently streaming", 60*60, new TvContentRating[] {rating}, new String[] {TvContract.Programs.Genres.NEWS}, streamUrl, TvInputPlayer.SOURCE_TYPE_HTTP_PROGRESSIVE, 0));
        return infoList;
    }
    public List<TvManager.ProgramInfo> getPrograms(JSONChannel jsonChannel) {
        List<TvManager.ProgramInfo> infoList = new ArrayList<>();
        String streamUrl = jsonChannel.getUrl();
        infoList.add(new TvManager.ProgramInfo(jsonChannel.getName()+" Live", jsonChannel.getLogo(),
                "Currently streaming", 60*60, new TvContentRating[] {rating}, new String[] {TvContract.Programs.Genres.NEWS}, streamUrl, TvInputPlayer.SOURCE_TYPE_HTTP_PROGRESSIVE, 0));
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
                if(jsonChannel.getNumber().equals(channelNumber))
                    return jsonChannel;
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
        JSONArray channels = obj.getJSONArray("channels");
        channels.put(n00b.toJSON());
        save();
    }
    public void update(JSONChannel n00b) throws JSONException {
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
                        jsonArray.put(i, n00b.toJSON());
                        save();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor e = sp.edit();
        e.putString(KEY, toString());
        e.commit();
    }
    @Override
    public String toString() {
        return obj.toString();
    }
}
