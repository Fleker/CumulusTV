package com.felkertech.n.fileio;

import android.content.Context;
import android.util.Log;

import com.felkertech.n.cumulustv.model.ChannelDatabase;

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
 * Created by Nick on 11/25/2015.
 */
public class M3UParser {
    private static final String TAG = "cumulus:M3UParser";
    public static TvListing parse(InputStream inputStream, Context mContext) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        List<XmlTvChannel> channels = new ArrayList<>();
        List<XmlTvProgram> programs = new ArrayList<>();
        Map<Integer, Integer> channelMap = new HashMap<>();

        while ((line = in.readLine()) != null) {
            if (line.startsWith("#EXTINF:")) {
                // #EXTINF:0051 tvg-id="blizz.de" group-title="DE Spartensender" tvg-logo="897815.png", [COLOR orangered]blizz TV HD[/COLOR]

                String id = null;
                String displayName = null;
                String displayNumber = null;
                int originalNetworkId = 0;
                XmlTvIcon icon = null;

                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    for (String part : parts[0].split(" ")) {
                        if (part.startsWith("#EXTINF:")) {
//                            Log.d(TAG, "Part: "+part);
                            displayNumber = part.substring(8).replaceAll("^0+", "");
//                            Log.d(TAG, "Display Number: "+displayNumber);
                            if(displayNumber.isEmpty())
                                displayNumber = ChannelDatabase.getAvailableChannelNumber(mContext)+"";
                            if(displayNumber.equals("-1"))
                                displayNumber = ChannelDatabase.getAvailableChannelNumber(mContext)+"";
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
                    XmlTvChannel channel =
                            new XmlTvChannel(id, displayName, displayNumber, icon,
                                    originalNetworkId, 0, 0, false);
                    if (channelMap.containsKey(originalNetworkId)) {
                        int freeChannel = 1;
                        while(channelMap.containsKey(new Integer(freeChannel))) {
                            freeChannel++;
                        }
//                        channels.set(channelMap.get(new Integer(freeChannel)), channel);
                        channelMap.put(freeChannel, channels.size());
                        channel.displayNumber = freeChannel+"";
//                        if(channel.url != null)
                            channels.add(channel);
                    } else {
                        channelMap.put(originalNetworkId, channels.size());
//                        if(channel.url != null)
                            channels.add(channel);
                    }
                } else {
                    Log.d(TAG, "Import failed: "+originalNetworkId+"= "+line);
                }
            } else if (line.startsWith("http") && channels.size() > 0) {
                channels.get(channels.size()-1).url = line;
            } else if(line.startsWith("rtmp") && channels.size() > 0) {
                channels.get(channels.size()-1).url = line;
            }
        }
        TvListing tvl = new TvListing(channels, programs);
        Log.d(TAG, "Done parsing");
        Log.d(TAG, tvl.toString());
        return new TvListing(channels, programs);
    }
    public static class TvListing {
        public List<XmlTvChannel> channels;
        public final List<XmlTvProgram> programs;

        public TvListing(List<XmlTvChannel> channels, List<XmlTvProgram> programs) {
            this.channels = channels;
            //Validate channels, making sure they have urls
            Iterator<XmlTvChannel> xmlTvChannelIterator = channels.iterator();
            while(xmlTvChannelIterator.hasNext()) {
                XmlTvChannel tvChannel = xmlTvChannelIterator.next();
                if(tvChannel.url == null) {
                    Log.e(TAG, tvChannel.displayName+" has no url!");
                    xmlTvChannelIterator.remove();
                }

            }
            this.programs = programs;
        }

        public void setChannels(List<XmlTvChannel> channels) {
            this.channels = channels;
        }

        @Override
        public String toString() {
            String out = "";
            for(XmlTvChannel tvChannel: channels) {
                out += tvChannel.toString();
            }
            return out;
        }

        public String getChannelList() {
            String out = "";
            for(XmlTvChannel tvChannel: channels) {
                out += tvChannel.displayNumber+" - "+tvChannel.displayName+"\n";
            }
            return out;
        }
    }
    public static class XmlTvChannel {
        public final String id;
        public final String displayName;
        public String displayNumber;
        public final XmlTvIcon icon;
        public final int originalNetworkId;
        public final int transportStreamId;
        public final int serviceId;
        public final boolean repeatPrograms;
        public String url;

        public XmlTvChannel(String id, String displayName, String displayNumber, XmlTvIcon icon,
                            int originalNetworkId, int transportStreamId, int serviceId,
                            boolean repeatPrograms) {
            this(id, displayName, displayNumber, icon, originalNetworkId, transportStreamId,
                    serviceId, repeatPrograms, null);
        }

        public XmlTvChannel(String id, String displayName, String displayNumber, XmlTvIcon icon,
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
