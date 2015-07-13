package com.felkertech.n.cumulustv;

import android.media.tv.TvContentRating;

import java.util.List;

/**
 * Created by N on 7/12/2015.
 */
public class TvManager {
    private static final String VALUE_VIDEO_TYPE_HTTP_PROGRESSIVE = "HTTP_PROGRESSIVE";
    private static final String VALUE_VIDEO_TYPE_HLS = "HLS";
    private static final String VALUE_VIDEO_TYPE_MPEG_DASH = "MPEG_DASH";

    public static final class ChannelInfo {
        public final String number;
        public final String name;
        public final String logoUrl;
        public final int originalNetworkId;
        public final int transportStreamId;
        public final int serviceId;
        public final int videoWidth;
        public final int videoHeight;
        public final List<ProgramInfo> programs;

        public ChannelInfo(String number, String name, String logoUrl, int originalNetworkId,
                           int transportStreamId, int serviceId, int videoWidth, int videoHeight,
                           List<ProgramInfo> programs) {
            this.number = number;
            this.name = name;
            this.logoUrl = logoUrl;
            this.originalNetworkId = originalNetworkId;
            this.transportStreamId = transportStreamId;
            this.serviceId = serviceId;
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.programs = programs;
        }
    }

    public static final class ProgramInfo {
        public final String title;
        public final String posterArtUri;
        public final String description;
        public final long durationSec;
        public final String videoUrl;
        public final int videoType;
        public final int resourceId;
        public final TvContentRating[] contentRatings;
        public final String[] genres;

        public ProgramInfo(String title, String posterArtUri, String description, long durationSec,
                           TvContentRating[] contentRatings, String[] genres, String videoUrl, int videoType, int resourceId) {
            this.title = title;
            this.posterArtUri = posterArtUri;
            this.description = description;
            this.durationSec = durationSec;
            this.contentRatings = contentRatings;
            this.genres = genres;
            this.videoUrl = videoUrl;
            this.videoType = videoType;
            this.resourceId = resourceId;
        }
    }

    public static final class PlaybackInfo {
        public final long startTimeMs;
        public final long endTimeMs;
        public final String videoUrl;
        public final int videoType;
        public final TvContentRating[] contentRatings;

        public PlaybackInfo(long startTimeMs, long endTimeMs, String videoUrl, int videoType,
                            TvContentRating[] contentRatings) {
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
            this.contentRatings = contentRatings;
            this.videoUrl = videoUrl;
            this.videoType = videoType;
        }
    }

    public static final class TvInput {
        public final String displayName;
        public final String name;
        public final String description;
        public final String logoThumbUrl;
        public final String logoBackgroundUrl;

        public TvInput(String displayName,
                       String name,
                       String description,
                       String logoThumbUrl,
                       String logoBackgroundUrl) {
            this.displayName = displayName;
            this.name = name;
            this.description = description;
            this.logoThumbUrl = logoThumbUrl;
            this.logoBackgroundUrl = logoBackgroundUrl;
        }
    }
}
