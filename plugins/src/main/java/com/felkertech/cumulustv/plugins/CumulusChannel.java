package com.felkertech.cumulustv.plugins;

import android.content.ComponentName;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * A CumulusChannel is a representation of a streaming channel with a variety of different pieces
 * of metadata related to the channel.
 *
 * @author Nick
 * @version 2016.09.04
 */
public class CumulusChannel implements JsonContainer {
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

    protected CumulusChannel() {
    }

    public boolean isAudioOnly() {
        return audioOnly;
    }

    public String getEpgUrl() {
        return epgUrl;
    }

    public String getGenresString() {
        if (genres == null) {
            return "";
        }
        return genres;
    }

    public boolean hasLogo() {
        return getLogo() != null && !getLogo().isEmpty();
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

    public JSONObject toJson() throws JSONException {
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

    public String toString() {
        try {
            return toJson().toString();
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CumulusChannel) {
            CumulusChannel other = (CumulusChannel) o;
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
        private CumulusChannel cumulusChannel;

        public Builder() {
            cumulusChannel = new CumulusChannel();
        }

        public Builder(String string) throws JSONException {
            this(new JSONObject(string));
        }

        public Builder(CumulusChannel channel) {
            cumulusChannel = new CumulusChannel();
            cumulusChannel.audioOnly = channel.audioOnly;
            cumulusChannel.epgUrl = channel.epgUrl;
            cumulusChannel.genres = channel.genres;
            cumulusChannel.logo = channel.logo;
            cumulusChannel.mediaUrl = channel.mediaUrl;
            cumulusChannel.name = channel.name;
            cumulusChannel.number = channel.number;
            cumulusChannel.pluginSource = channel.pluginSource;
            cumulusChannel.splashscreen = channel.splashscreen;
        }

        public Builder(JSONObject jsonObject) throws JSONException {
            cumulusChannel = new CumulusChannel();
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
            cumulusChannel.audioOnly = audioOnly;
            return this;
        }

        public Builder setEpgUrl(String epgUrl) {
            cumulusChannel.epgUrl = epgUrl;
            return this;
        }

        public Builder setGenres(String genres) {
            cumulusChannel.genres = genres;
            return this;
        }

        public Builder setLogo(String logo) {
            cumulusChannel.logo = logo;
            return this;
        }

        public Builder setMediaUrl(String mediaUrl) {
            cumulusChannel.mediaUrl = mediaUrl;
            return this;
        }

        public Builder setName(String name) {
            cumulusChannel.name = name;
            return this;
        }

        public Builder setNumber(String number) {
            cumulusChannel.number = number;
            return this;
        }

        public Builder setPluginSource(@NonNull ComponentName pluginComponent) {
            if (pluginComponent != null) {
                cumulusChannel.pluginSource = pluginComponent.flattenToString();
            }
            return this;
        }

        public Builder setPluginSource(String pluginComponentName) {
            cumulusChannel.pluginSource = pluginComponentName;
            return this;
        }

        public Builder setSplashscreen(String splashscreen) {
            cumulusChannel.splashscreen = splashscreen;
            return this;
        }

        public CumulusChannel build() {
            if (cumulusChannel.name == null || cumulusChannel.name.isEmpty()) {
                throw new IllegalArgumentException("Name must be defined.");
            }
            if (cumulusChannel.number == null || cumulusChannel.number.isEmpty()) {
                throw new IllegalArgumentException("Number must be defined.");
            }
            if (cumulusChannel.mediaUrl == null || cumulusChannel.mediaUrl.isEmpty()) {
                throw new IllegalArgumentException("Url must be defined.");
            }
            return cumulusChannel;
        }

        /**
         * If you want a specific channel to be used in the builder, call this method first and
         * it will replace the object. This may be useful if you're extending this class.
         *
         * @param cumulusChannel The object you want to use.
         */
        protected void setCumulusChannel(CumulusChannel cumulusChannel) {
            this.cumulusChannel = cumulusChannel;
        }

        /**
         * Gets the object you've been using in the builder. This may be useful if you're extending
         * this class.
         *
         * @return The currently build object.
         */
        protected CumulusChannel getCumulusChannel() {
            return cumulusChannel;
        }

        protected void cloneInto(CumulusChannel channel) {
            channel.audioOnly = cumulusChannel.audioOnly;
            channel.epgUrl = cumulusChannel.epgUrl;
            channel.genres = cumulusChannel.genres;
            channel.logo = cumulusChannel.logo;
            channel.mediaUrl = cumulusChannel.mediaUrl;
            channel.name = cumulusChannel.name;
            channel.number = cumulusChannel.number;
            channel.pluginSource = cumulusChannel.pluginSource;
            channel.splashscreen = cumulusChannel.splashscreen;
        }
    }
}
