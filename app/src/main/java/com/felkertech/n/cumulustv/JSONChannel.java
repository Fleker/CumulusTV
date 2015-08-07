package com.felkertech.n.cumulustv;

import android.media.tv.TvContract;

import com.felkertech.n.boilerplate.Utils.CommaArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class JSONChannel {
    private String number;
    private String name;
    private String url;
    private String logo;
    private String splashscreen;
    private String genres;
    private String source;
    public JSONChannel(JSONObject jsonObject) {
        try {
            number = jsonObject.getString("number");
            name = jsonObject.getString("name");
            url = jsonObject.getString("url");
            if(jsonObject.has("logo"))
                logo = jsonObject.getString("logo");
            else
                logo = "";
            if(jsonObject.has("splashscreen"))
                splashscreen = jsonObject.getString("splashscreen");
            else
                splashscreen = "";
            if(jsonObject.has("genres"))
                genres = jsonObject.getString("genres");
            else
                genres = TvContract.Programs.Genres.LIFE_STYLE;
            if(jsonObject.has("source"))
                source = jsonObject.getString("source");
            else
                source = "";
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public JSONChannel(String number, String name, String url, String logo, String splash, String genres) {
        this.number = number;
        this.name = name;
        this.url = url;
        this.logo = logo;
        this.splashscreen = splash;
        this.genres = genres;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getLogo() {
        return logo;
    }

    public String getSplashscreen() {
        return splashscreen;
    }

    public boolean hasLogo() {
        return !getLogo().isEmpty();
    }
    public boolean hasSplashscreen() {
        return !getSplashscreen().isEmpty();
    }

    public String getSource() {
        return source;
    }
    public boolean hasSource() {
        return !getSource().isEmpty();
    }

    public void setSource(String source) {
        this.source = source;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("number", getNumber());
        object.put("name", getName());
        object.put("logo", getLogo());
        object.put("url", getUrl());
        object.put("splashscreen", getSplashscreen());
        object.put("genres", getGenresString());
        object.put("source", getSource());
        return object;
    }
    public String toString() {
        try {
            return toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getGenresString() {
        return genres;
    }
    public String[] getGenres() {
        if(genres.isEmpty())
            return new String[]{TvContract.Programs.Genres.LIFE_STYLE};
        else {
            //Parse genres
            CommaArray ca = new CommaArray(genres);
            Iterator<String> it = ca.getIterator();
            ArrayList<String> arrayList = new ArrayList<>();
            while(it.hasNext()) {
                String i = it.next();
                arrayList.add(i);
            }
            return arrayList.toArray(new String[arrayList.size()]);
        }
    }
}
