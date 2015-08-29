package com.felkertech.n.cumulustv.xmltv;

/**
 * Created by guest1 on 8/25/2015.
 */
public class Program {
    String channelId;
    long start;
    long end;
    String title;
    String subtitle;
    String description;
    long airDate;
    public Program(String channelId, long start, long end) {
        this.channelId = channelId;
        this.start = start;
        this.end = end;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getAirDate() {
        return airDate;
    }

    public void setAirDate(long airDate) {
        this.airDate = airDate;
    }

    public String getChannelId() {
        return channelId;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    @Override
    public String toString() {
        return "'"+getTitle()+"': "+getDescription();
    }
}
