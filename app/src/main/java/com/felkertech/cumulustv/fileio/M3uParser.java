package com.felkertech.cumulustv.fileio;

import android.content.Context;
import android.util.Log;

import com.felkertech.cumulustv.model.ChannelDatabase;

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
            if (haystack.indexOf(n) > -1) {
                return haystack.indexOf(n);
            }
        }
        return -1;
    }

    public static TvListing parse(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        List<M3uTvChannel> channels = new ArrayList<>();
        List<XmlTvProgram> programs = new ArrayList<>();
        Map<Integer, Integer> channelMap = new HashMap<>();

        while ((line = in.readLine()) != null) {
            if (line.startsWith("#EXTINF:")) {
                // #EXTINF:0051 tvg-id="blizz.de" group-title="DE Spartensender" tvg-logo="897815.png", [COLOR orangered]blizz TV HD[/COLOR]
                M3uTvChannel channel = new M3uTvChannel();


                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    for (String part : parts[0].split(" ")) {
                        int valueDivider = indexOf(part, ":", "=");

                        if (part.startsWith("#EXTINF:")) {
                            displayNumber = part.substring(8).replaceAll("^0+", "");
                            if (displayNumber.isEmpty()) {
                                displayNumber = String.valueOf(channels.size());
                            }
                            if (displayNumber.equals("-1")) {
                                displayNumber = String.valueOf(channels.size());
                            }
                            originalNetworkId = Integer.parseInt(displayNumber);
                        } else if (part.startsWith("tvg-id=")) {
                            int end = part.indexOf("\"", 8);
                            if (end > 8) {
                                id = part.substring(8, end);
                            }
                        } else if (part.startsWith("tvg-logo=")) {
                            int end = part.indexOf("\"", 10);
                            if (end > 10) {
                                icon = new XmlTvIcon("http://logo.iptv.ink/"
                                        + part.substring(10, end));
                            }
                        }
                    }
                    displayName = parts[1].replaceAll("\\[\\/?(COLOR |)[^\\]]*\\]", "");
                }

                if (originalNetworkId != 0 && displayName != null) {
                    M3uTvChannel channel =
                            new M3uTvChannel(id, displayName, displayNumber, icon,
                                    originalNetworkId, 0, 0, false);
                    if (channelMap.containsKey(originalNetworkId)) {
                        int freeChannel = 1;
                        while (channelMap.containsKey(Integer.valueOf(freeChannel))) {
                            freeChannel++;
                        }
                        channelMap.put(freeChannel, channels.size());
                        channel.displayNumber = freeChannel + "";
                        channels.add(channel);
                    } else {
                        channelMap.put(originalNetworkId, channels.size());
                        channels.add(channel);
                    }
                } else {
                    Log.d(TAG, "Import failed: " + originalNetworkId + "= " + line);
                }

                line = in.readLine();
                if (line.startsWith("http") && channels.size() > 0) {
                    channels.get(channels.size() - 1).url = line;
                } else if (line.startsWith("rtmp") && channels.size() > 0) {
                    channels.get(channels.size() - 1).url = line;
                }
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
            //Validate channels, making sure they have urls
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
        public XmlTvIcon icon;
        public int originalNetworkId;
        public int transportStreamId;
        public int serviceId;
        public boolean repeatPrograms;
        public String url;
        private HashMap<String, String> m3uAttributes;

        public M3uTvChannel() {

        }

        public M3uTvChannel(String id, String displayName, String displayNumber, XmlTvIcon icon,
                            int originalNetworkId, int transportStreamId, int serviceId,
                            boolean repeatPrograms) {
            this(id, displayName, displayNumber, icon, originalNetworkId, transportStreamId,
                    serviceId, repeatPrograms, null);
        }

        public M3uTvChannel(String id, String displayName, String displayNumber, XmlTvIcon icon,
                            int originalNetworkId, int transportStreamId, int serviceId,
                            boolean repeatPrograms, String url) {
            this.id = id;
            this.displayName = displayName;
            this.displayNumber = displayNumber;
            this.icon = icon;
            this.originalNetworkId = originalNetworkId;
            this.transportStreamId = transportStreamId;
            this.serviceId = serviceId;
            this.repeatPrograms = repeatPrograms;
            this.url = url;
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

    public static class XmlTvIcon {
        public final String src;

        public XmlTvIcon(String src) {
            this.src = src;
        }
    }

    public static class XmlTvRating {
        public final String system;
        public final String value;

        public XmlTvRating(String system, String value) {
            this.system = system;
            this.value = value;
        }
    }
}
