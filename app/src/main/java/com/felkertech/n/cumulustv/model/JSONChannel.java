package com.felkertech.n.cumulustv.model;

import android.content.ComponentName;
import android.media.tv.TvContract;
import android.support.annotation.NonNull;


import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.settingsmanager.common.CommaArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class JsonChannel {
    private static final String KEY_AUDIO_ONLY = "audioOnly";
    private static final String KEY_EPG_URL = "epgUrl";
    private static final String KEY_GENRES = "genres";
    private static final String KEY_LOGO = "logo";
    private static final String KEY_MEDIA_URL = "url";
    private static final String KEY_NAME = "name";
    private static final String KEY_NUMBER = "number";
    private static final String KEY_PLUGIN_SOURCE = "service";
    private static final String KEY_SPLASHSCREEN = "splashscreen";

    private boolean audioOnly;
    private String epgUrl;
    private String genres;
    private String logo;
    private String mediaUrl;
    private String name;
    private String number;
    private String pluginSource;
    private String splashscreen;

    private JsonChannel() {
    }

    public boolean isAudioOnly() {
        return audioOnly;
    }

    public String getEpgUrl() {
        return epgUrl;
    }

    public String getGenresString() {
        return genres;
    }

    public String[] getGenres() {
        if(genres == null) {
            return new String[]{TvContract.Programs.Genres.MOVIES};
        }
        if(genres.isEmpty()) {
            return new String[]{TvContract.Programs.Genres.MOVIES};
        }
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

    public boolean hasLogo() {
        return !getLogo().isEmpty();
    }

    public String getLogo() {
        return logo;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public ComponentName getPluginSource() {
        if (pluginSource != null) {
            return ComponentName.unflattenFromString(pluginSource);
        }
        return null;
    }

    public boolean hasSplashscreen() {
        return getSplashscreen() != null && !getSplashscreen().isEmpty();
    }

    public String getSplashscreen() {
        return splashscreen;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(KEY_AUDIO_ONLY, isAudioOnly());
        object.put(KEY_EPG_URL, getEpgUrl());
        object.put(KEY_GENRES, getGenresString());
        object.put(KEY_LOGO, getLogo());
        object.put(KEY_MEDIA_URL, getMediaUrl());
        object.put(KEY_NAME, getName());
        object.put(KEY_NUMBER, getNumber());
        object.put(KEY_PLUGIN_SOURCE, pluginSource);
        object.put(KEY_SPLASHSCREEN, getSplashscreen());
        return object;
    }

    public Channel toChannel() {
        Channel channel = new Channel();
        channel.setNumber(getNumber());
        channel.setName(getName());
        channel.setLogoUrl(getLogo());
        channel.setInternalProviderData(getMediaUrl());
        channel.setOriginalNetworkId(getMediaUrl().hashCode());
        return channel;
    }

    public String toString() {
        try {
            return toJSON().toString();
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JsonChannel) {
            JsonChannel other = (JsonChannel) o;
            return Objects.equals(getNumber(), other.getNumber()) &&
                    Objects.equals(getName(), other.getName()) &&
                    Objects.equals(getLogo(), other.getLogo()) &&
                    Objects.equals(getEpgUrl(), other.getEpgUrl()) &&
                    Objects.equals(getSplashscreen(), other.getSplashscreen()) &&
                    Objects.equals(getGenresString(), other.getGenresString()) &&
                    Objects.equals(getMediaUrl(), other.getMediaUrl());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNumber(), getName(), getLogo(), getEpgUrl(), getSplashscreen(),
                getGenresString(), getMediaUrl());
    }

    public static class Builder {
        private JsonChannel jsonChannel;

        public Builder() {
            jsonChannel = new JsonChannel();
        }

        public Builder(String string) throws JSONException {
            this(new JSONObject(string));
        }

        public Builder(JsonChannel channel) {
            // Clone
            jsonChannel = new JsonChannel();
            jsonChannel.audioOnly = channel.audioOnly;
            jsonChannel.epgUrl = channel.epgUrl;
            jsonChannel.genres = channel.genres;
            jsonChannel.logo = channel.logo;
            jsonChannel.mediaUrl = channel.mediaUrl;
            jsonChannel.name = channel.name;
            jsonChannel.number = channel.number;
            jsonChannel.pluginSource = channel.pluginSource;
            jsonChannel.splashscreen = channel.splashscreen;
        }

        public Builder(JSONObject jsonObject) throws JSONException {
            jsonChannel = new JsonChannel();
            if (jsonObject.has(KEY_AUDIO_ONLY)) {
                setAudioOnly(jsonObject.getBoolean(KEY_AUDIO_ONLY));
            }
            if (jsonObject.has(KEY_EPG_URL)) {
                setEpgUrl(jsonObject.getString(KEY_EPG_URL));
            }
            if (jsonObject.has(KEY_GENRES)) {
                setGenres(jsonObject.getString(KEY_GENRES));
            }
            if (jsonObject.has(KEY_LOGO)) {
                setLogo(jsonObject.getString(KEY_LOGO));
            }
            if (jsonObject.has(KEY_NAME)) {
                setName(jsonObject.getString(KEY_NAME));
            }
            if (jsonObject.has(KEY_NUMBER)) {
                setNumber(jsonObject.getString(KEY_NUMBER));
            }
            if (jsonObject.has(KEY_MEDIA_URL)) {
                setMediaUrl(jsonObject.getString(KEY_MEDIA_URL));
            }
            if (jsonObject.has(KEY_SPLASHSCREEN)) {
                setSplashscreen(jsonObject.getString(KEY_SPLASHSCREEN));
            }
            if (jsonObject.has(KEY_PLUGIN_SOURCE)) {
                setPluginSource(ComponentName.unflattenFromString(
                        jsonObject.getString(KEY_PLUGIN_SOURCE)));
            }
        }

        public Builder setAudioOnly(boolean audioOnly) {
            jsonChannel.audioOnly = audioOnly;
            return this;
        }

        public Builder setEpgUrl(String epgUrl) {
            jsonChannel.epgUrl = epgUrl;
            return this;
        }

        public Builder setGenres(String genres) {
            jsonChannel.genres = genres;
            return this;
        }

        public Builder setLogo(String logo) {
            jsonChannel.logo = logo;
            return this;
        }

        public Builder setMediaUrl(String mediaUrl) {
            jsonChannel.mediaUrl = mediaUrl;
            return this;
        }

        public Builder setName(String name) {
            jsonChannel.name = name;
            return this;
        }

        public Builder setNumber(String number) {
            jsonChannel.number = number;
            return this;
        }

        public Builder setPluginSource(@NonNull ComponentName pluginComponent) {
            jsonChannel.pluginSource = pluginComponent.flattenToString();
            return this;
        }

        public Builder setPluginSource(String pluginComponentName) {
            jsonChannel.pluginSource = pluginComponentName;
            return this;
        }

        public Builder setSplashscreen(String splashscreen) {
            jsonChannel.splashscreen = splashscreen;
            return this;
        }

        public JsonChannel build() {
            if (jsonChannel.name == null || jsonChannel.name.isEmpty()) {
                throw new IllegalArgumentException("Name must be defined.");
            }
            if (jsonChannel.number == null || jsonChannel.number.isEmpty()) {
                throw new IllegalArgumentException("Number must be defined.");
            }
            if (jsonChannel.mediaUrl == null || jsonChannel.mediaUrl.isEmpty()) {
                throw new IllegalArgumentException("Url must be defined.");
            }
            return jsonChannel;
        }
    }
}
