package com.felkertech.cumulustv.fileio;

import android.content.Context;
import android.util.Log;

import com.felkertech.cumulustv.model.ChannelDatabase;
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

/**
 * This class is responsible to converting between M3u playlists and the application model.
 */
public class M3uParser {
    private static final String TAG = M3uParser.class.getSimpleName();

    private static int indexOf(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return haystack.indexOf(n);
            }
        }
        return -1;
    }

    private static String getKey(HashMap<String, String> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) {
                return map.get(k);
            }
        }
        return null;
    }

    public static TvListing parse(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        List<M3uTvChannel> channels = new ArrayList<>();
        List<XmlTvProgram> programs = new ArrayList<>();
        Map<Integer, Integer> channelMap = new HashMap<>();
        Map<String, String> globalAttributes = new HashMap<>(); // Unused for now

        while ((line = in.readLine()) != null) {
            if (line.startsWith("#EXTINF:")) { // This is a channel
                // #EXTINF:0051 tvg-id="blizz.de" group-title="DE Spartensender" tvg-logo="897815.png", [COLOR orangered]blizz TV HD[/COLOR]
                M3uTvChannel channel = new M3uTvChannel();

                String[] parts = line.split(",", 2);
                String channelAttributes = parts[0];
                while (channelAttributes.length() > 0) { // Chip away at data until complete
                    boolean inPhrase = false;
                    int valueDivider = indexOf(channelAttributes, ":", "=");
                    String attribute = channelAttributes.substring(0, valueDivider);
                    int valueIndex = valueDivider + 1;
                    int valueEnd = channelAttributes.indexOf(" ", valueIndex);
                    int variableEnd = valueEnd + 1;
                    if (attribute.charAt(valueDivider + 1) == '"') {
                        valueIndex++;
                        inPhrase = true;
                        valueEnd = channelAttributes.indexOf("\"", valueIndex + 1);
                        variableEnd = valueEnd + 2; // '" '
                    }
                    String value = channelAttributes.substring(valueIndex, valueEnd);
                    channel.put(attribute, value);
                    channelAttributes = channelAttributes.substring(variableEnd).trim();
                }
                String channelName = parts[1].replaceAll("\\[\\/?(COLOR |)[^\\]]*\\]", "");

                line = in.readLine();
                if (line.startsWith("http") && channels.size() > 0) {
                    channel.url = line;
                } else if (line.startsWith("rtmp") && channels.size() > 0) {
                    channel.url = line;
                }

                // Set channel properties
                channel.displayName = channelName;
                channel.displayNumber = getKey(channel.m3uAttributes, "#EXTINF", "tvg-id");
                channel.icon = getKey(channel.m3uAttributes, "tvg-logo");
            }
        }
        TvListing tvl = new TvListing(channels, programs);
        Log.d(TAG, "Done parsing");
        Log.d(TAG, tvl.toString());
        return new TvListing(channels, programs);
    }

    public static class TvListing {
        public List<M3uTvChannel> channels;
        public final List<XmlTvProgram> programs;

        public TvListing(List<M3uTvChannel> channels, List<XmlTvProgram> programs) {
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
            this.programs = programs;
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
                out += tvChannel.displayNumber+" - "+tvChannel.displayName+"\n";
            }
            return out;
        }
    }

    public static class M3uTvChannel {
        public String id;
        public String displayName;
        public String displayNumber;
        public String icon;
        public int originalNetworkId;
        public int transportStreamId;
        public int serviceId;
        public boolean repeatPrograms;
        public String url;
        private HashMap<String, String> m3uAttributes = new HashMap<>();

        public M3uTvChannel() {

        }

        public void put(String key, String value) {
            m3uAttributes.put(key, value);
        }

        public JsonChannel toJsonChannel() {
            return new JsonChannel.Builder()
                    .setLogo(icon)
                    .setName(displayName)
                    .setNumber(displayNumber)
                    .setMediaUrl(url)
                    .build();
        }

        @Override
        public String toString() {
            return displayNumber+" - "+displayName+": "+url+"\n";
        }
    }

    public static class XmlTvProgram {
        public final String channelId;
        public final String title;
        public final String description;
        public final XmlTvIcon icon;
        public final String[] category;
        public final long startTimeUtcMillis;
        public final long endTimeUtcMillis;
        public final XmlTvRating[] rating;
        public final String videoSrc;
        public final int videoType;

        private XmlTvProgram(String channelId, String title, String description, XmlTvIcon icon,
                             String[] category, long startTimeUtcMillis, long endTimeUtcMillis,
                             XmlTvRating[] rating, String videoSrc, int videoType) {
            this.channelId = channelId;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.category = category;
            this.startTimeUtcMillis = startTimeUtcMillis;
            this.endTimeUtcMillis = endTimeUtcMillis;
            this.rating = rating;
            this.videoSrc = videoSrc;
            this.videoType = videoType;
        }

        public long getDurationMillis() {
            return endTimeUtcMillis - startTimeUtcMillis;
        }
    }

    @Deprecated
    public static class XmlTvIcon {
        public final String src;

        public XmlTvIcon(String src) {
            this.src = src;
        }
    }

    @Deprecated
    public static class XmlTvRating {
        public final String system;
        public final String value;

        public XmlTvRating(String system, String value) {
            this.system = system;
            this.value = value;
        }
    }
}
