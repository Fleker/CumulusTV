package com.felkertech.cumulustv.fileio;

import android.util.Log;

import com.felkertech.cumulustv.model.JsonChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.felkertech.cumulustv.fileio.M3uParser.Constants.CH_AUDIO_ONLY;
import static com.felkertech.cumulustv.fileio.M3uParser.Constants.CH_EPG_URL;
import static com.felkertech.cumulustv.fileio.M3uParser.Constants.CH_GENRES;
import static com.felkertech.cumulustv.fileio.M3uParser.Constants.CH_GENRES_ALT1;
import static com.felkertech.cumulustv.fileio.M3uParser.Constants.CH_LOGO;
import static com.felkertech.cumulustv.fileio.M3uParser.Constants.CH_NUMBER;
import static com.felkertech.cumulustv.fileio.M3uParser.Constants.CH_PLUGIN;
import static com.felkertech.cumulustv.fileio.M3uParser.Constants.CH_SPLASH;
import static com.felkertech.cumulustv.fileio.M3uParser.Constants.CH_SPLASH_ALT1;
import static com.felkertech.cumulustv.fileio.M3uParser.Constants.KEY_COUNTRY;

/**
 * This class is responsible to converting between M3u playlists and the application model.
 */
public class M3uParser {
    private static final String TAG = M3uParser.class.getSimpleName();

    private static int indexOf(String haystack, String... needles) {
        int index = haystack.length();
        for (String n : needles) {
            int needleIndex = haystack.indexOf(n);
            index = (index > needleIndex && needleIndex > -1) ? needleIndex : index;
        }
        return index;
    }

    private static int indexOf(String haystack, int startAt, String... needles) {
        int index = haystack.length();
        for (String n : needles) {
            int needleIndex = haystack.indexOf(n, startAt);
            index = (index > needleIndex && needleIndex > -1) ? needleIndex : index;
        }
        return index;
    }

    private static String getKey(HashMap<String, String> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) {
                return map.get(k);
            }
        }
        return null;
    }

    private static int getLastComma(String haystack) {
        int comma = -1;
        for (int i = 0; i < haystack.length(); i++) {
            int c2 = haystack.indexOf(",", comma + 1);
            comma = (c2 > comma) ? c2 : comma;
        }
        return comma;
    }

    public static TvListing parse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        List<M3uTvChannel> channels = new ArrayList<>();
        Map<String, String> globalAttributes = new HashMap<>(); // Unused for now
        // Check the first line.
        line = in.readLine();
        Log.d(TAG, "First line is " + line);
        if (line.startsWith("#EXTM3U")) {
            // Parse global attributes
        } else {
            // Not an M3u playlist
            return null;
        }

        while ((line = in.readLine()) != null) {
            if (line.startsWith("#EXTINF:")) { // This is a channel
                M3uTvChannel channel = new M3uTvChannel();
                String channelAttributes = line.substring(0, getLastComma(line));
                String channelName = line.substring(getLastComma(line) + 1).trim()
                        .replaceAll("\\[\\/?(COLOR |)[^\\]]*\\]", "");
                while (channelAttributes.length() > 0) { // Chip away at data until complete
                    Log.d(TAG, channelAttributes);
                    int valueDivider = indexOf(channelAttributes, ":", "=");
                    String attribute = channelAttributes.substring(0, valueDivider);
                    int valueIndex = valueDivider + 1;
                    int valueEnd = indexOf(channelAttributes, valueIndex, " ");
                    int variableEnd = valueEnd + 1;
                    if (valueEnd == -1) {
                        valueEnd = channelAttributes.length(); // We're at the end
                    }
                    try {
                        if (channelAttributes.charAt(valueDivider + 1) == '"') {
                            valueIndex++;
                            valueEnd = channelAttributes.indexOf("\"", valueIndex + 1);
                            variableEnd = valueEnd + 2; // '" '
                        }
                        String value = channelAttributes.substring(valueIndex, valueEnd);
                        channel.put(attribute, value);
                    } catch (StringIndexOutOfBoundsException e) {
                        throw new StringIndexOutOfBoundsException("Parsing error: " + channelAttributes
                            + " does not fit into founds " + valueIndex + " - " + valueEnd + " for" +
                                "line " + line);
                    }
                    if (variableEnd > channelAttributes.length()) {
                        channelAttributes = "";
                    } else {
                        channelAttributes = channelAttributes.substring(variableEnd).trim();
                    }
                }

                line = in.readLine();
                Log.d(TAG, "URL: " + line);
                if (line.startsWith("http")) {
                    channel.url = line;
                } else if (line.startsWith("rtmp")) {
                    channel.url = line;
                }

                // Set channel properties
                channel.displayName = channelName;
                channel.m3uAttributes.putAll(globalAttributes);
                channel.put("count", String.valueOf(channels.size()));
                channels.add(channel);
            } else if (line.startsWith("##")) {
                // Interpret as a country-group
                globalAttributes.put(KEY_COUNTRY, line.replaceAll("#", "").trim());
            }
        }
        TvListing tvl = new TvListing(channels);
        Log.d(TAG, "Done parsing");
        Log.d(TAG, tvl.toString());
        return new TvListing(channels);
    }

    public static class TvListing {
        public List<M3uTvChannel> channels;

        public TvListing(List<M3uTvChannel> channels) {
            this.channels = channels;
            // Validate channels, making sure they have urls
            Iterator<M3uTvChannel> xmlTvChannelIterator = channels.iterator();
            while(xmlTvChannelIterator.hasNext()) {
                M3uTvChannel tvChannel = xmlTvChannelIterator.next();
                if(tvChannel.url == null) {
                    Log.e(TAG, tvChannel.displayName+" has no url!");
                    xmlTvChannelIterator.remove();
                }

            }
        }

        public void setChannels(List<M3uTvChannel> channels) {
            this.channels = channels;
        }

        @Override
        public String toString() {
            String out = "";
            for(M3uTvChannel tvChannel: channels) {
                out += tvChannel.toString();
            }
            return out;
        }

        public String getChannelList() {
            String out = "";
            for(M3uTvChannel tvChannel: channels) {
                out += tvChannel.displayName+"\n";
            }
            return out;
        }
    }

    public static class M3uTvChannel {
        protected String displayName;
        public String url;
        private HashMap<String, String> m3uAttributes = new HashMap<>();

        public M3uTvChannel() {

        }

        public void put(String key, String value) {
            m3uAttributes.put(key, value);
        }

        public JsonChannel toJsonChannel() {
            return new JsonChannel.Builder()
                    .setAudioOnly(getKey(m3uAttributes, CH_AUDIO_ONLY) != null)
                    .setEpgUrl(getKey(m3uAttributes, CH_EPG_URL))
                    .setGenres(getKey(m3uAttributes, CH_GENRES, CH_GENRES_ALT1) +
                            "," + getKey(m3uAttributes, KEY_COUNTRY))
                    .setLogo(getKey(m3uAttributes, CH_LOGO))
                    .setName(displayName)
                    .setNumber(getKey(m3uAttributes, "#EXTINF:", CH_NUMBER, "count"))
                    .setPluginSource(getKey(m3uAttributes, CH_PLUGIN))
                    .setSplashscreen(getKey(m3uAttributes, CH_SPLASH, CH_SPLASH_ALT1))
                    .setMediaUrl(url)
                    .build();
        }

        @Override
        public String toString() {
            return toJsonChannel().toString() + "\n";
        }
    }

    public static class Constants {
        public static final String HEADER_TAG = "#EXTM3U";
        public static final String CHANNEL_TAG = "#EXTINF:-1";
        public static final String CH_NUMBER = "tvg-id";
        public static final String CH_LOGO = "tvg-logo";
        public static final String CH_AUDIO_ONLY = "audio-only";
        public static final String CH_EPG_URL = "epg-url";
        public static final String CH_GENRES = "group-title";
        public static final String CH_GENRES_ALT1 = "genres";
        public static final String CH_PLUGIN = "cumulus-plugin";
        public static final String CH_SPLASH = "splashscreen";
        public static final String CH_SPLASH_ALT1 = "tvg-splashscreen";

        public static final String KEY_COUNTRY = "country";
    }
}
