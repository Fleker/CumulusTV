package com.felkertech.cumulustv.model;

import android.media.tv.TvContract;

import com.felkertech.cumulustv.plugins.CumulusChannel;

/**
 * <p>This utility class is a single place to access the "Suggested Channels" that are seen in the
 * app. Having them in a separate place makes it easy to edit without affecting other parts
 * of the app.
 *
 * <p>The numbering scheme for these channels is a bit specific. They follow a rule:
 * </p>
 *
 * <ul>
 * <li>0xx NEWS</li>
 * <li>1xx SCIENCE / TECH / NATURE</li>
 * <li>2xx HISTORY / EDUCATION</li>
 * <li>3xx SPORTS / VIDEO GAMES</li>
 * <li>4xx MUSIC</li>
 * <li>5xx FICTION</li>
 * <li>6xx NONFICTION</li>
 * <li>7xx GOVERNMENT / SOCIETY</li>
 * <li>8xx ART / CULTURE / LANGUAGE</li>
 * <li>9xx MISC</li>
 * </ul>
 *
 * <p>
 * Some streams were found via <a href='http://rgw.ustream.tv/json.php/Ustream.searchBroadcast/'>
 *     UStream</a>.</p>
 */
public class SuggestedChannels {
    private static final CumulusChannel[] channels = {
            new JsonChannel.Builder()
                    .setGenres(TvContract.Programs.Genres.TECH_SCIENCE)
                    .setLogo("http://static-cdn1.ustream.tv/i/channel/live/1_6540154,256x144,b:20" +
                            "15071514.jpg")
                    .setMediaUrl("http://iphone-streaming.ustream.tv/uhls/6540154/streams/live/ip" +
                            "hone/playlist.m3u8")
                    .setName("NASA Public")
                    .setNumber("100")
                    .build(),
            new JsonChannel.Builder()
                    .setGenres(TvContract.Programs.Genres.TECH_SCIENCE)
                    .setLogo("http://static-cdn1.ustream.tv/i/channel/picture/9/4/0/8/9408562/940" +
                            "8562_iss_hr_1330361780,256x144,r:1.jpg")
                    .setMediaUrl("http://iphone-streaming.ustream.tv/uhls/9408562/streams/live/ip" +
                            "hone/playlist.m3u8")
                    .setName("ISS Stream")
                    .setNumber("101")
                    .build(),
            new JsonChannel.Builder()
                    .setGenres(TvContract.Programs.Genres.TECH_SCIENCE + "," +
                            TvContract.Programs.Genres.NEWS)
                    .setLogo("https://raw.githubusercontent.com/Fleker/CumulusTV/a576e4d49cec21ad" +
                            "1fdabb0ed49190f87800e462/store/other/TWiT-horizontal.png")
                    .setMediaUrl("http://hls.twit.tv/flosoft/mp4:twitStream_720/playlist.m3u8")
                    .setName("TWiT.tv")
                    .setNumber("133")
                    .build(),
            new JsonChannel.Builder()
                    .setGenres(TvContract.Programs.Genres.NEWS)
                    .setLogo("http://www.ewtn.com/images/ewtnLogo_new2016.png")
                    .setMediaUrl("http://ewtnmmd.mmdlive.lldns.net/ewtnmmd/a8d009b542d24f3faa63c1b" +
                            "d1c6620fb/manifest.m3u8")
                    .setName("EWTN")
                    .setNumber("135")
                    .setSplashscreen("http://www.ewtn.com/images/bg.jpg")
                    .build(),
            new JsonChannel.Builder()
                    .setGenres(TvContract.Programs.Genres.NEWS)
                    .setLogo("https://upload.wikimedia.org/wikipedia/en/c/cf/TelecareTV_JPEG.jpg")
                    .setMediaUrl("http://nlds7.neulion.com/nlds/telecare/telecare/as/live/telecare" +
                            "_hd_1600_ipad.m3u8")
                    .setName("Telecare")
                    .setNumber("137")
                    .setSplashscreen("http://www.telecaretv.org/fls/24700/site_graphics/siteBg2.jpg")
                    .build(),
            new JsonChannel.Builder()
                    .setLogo("http://static-cdn1.ustream.tv/i/channel/live/1_9600798,256x144,b:20" +
                            "15071514.jpg")
                    .setMediaUrl("http://iphone-streaming.ustream.tv/uhls/9600798/streams/live/ip" +
                            "hone/playlist.m3u8")
                    .setName("Monterey Bay Aquarium")
                    .setNumber("167")
                    .build(),
            new JsonChannel.Builder()
                    .setGenres(TvContract.Programs.Genres.MUSIC)
                    .setLogo("http://i.imgur.com/QRCIhN4.png")
                    .setMediaUrl("http://pablogott.videocdn.scaleengine.net/pablogott-iphone/play" +
                            "/ooftv1/playlist.m3u8")
                    .setNumber("400")
                    .setName("OutOfFocus.TV")
                    .build(),
            new JsonChannel.Builder()
                    .setAudioOnly(true)
                    .setGenres(TvContract.Programs.Genres.MUSIC + "," +
                            TvContract.Programs.Genres.ENTERTAINMENT)
                    .setLogo("https://ottleyboothr.files.wordpress.com/2015/06/beats-1.jpg")
                    .setMediaUrl("http://itsliveradio.apple.com/streams/master_session01_hub01_hu" +
                            "b02.m3u8")
                    .setName("Beats One Radio")
                    .setNumber("410")
                    .build(),
            new JsonChannel.Builder()
                    .setGenres(TvContract.Programs.Genres.ARTS + "," +
                            TvContract.Programs.Genres.ENTERTAINMENT)
                    .setLogo("http://content.provideocoalition.com/uploads/ArtbeatsLogo_blackbox." +
                            "jpg")
                    .setMediaUrl("http://cdn-fms.rbs.com.br/hls-vod/sample1_1500kbps.f4v.m3u8")
                    .setName("Artbeats Demo")
                    .setNumber("900")
                    .build()
    };

    public static CumulusChannel[] getSuggestedChannels() {
        return channels;
    }
}

/* new CumulusChannel("001",
        "Sky News",
        "https://www.youtube.com/embed/y60wDzZt8yg?autoplay=1",
        "http://news.sky.com/images/33dc2677.sky-news-logo.png", "",
        TvContract.Programs.Genres.NEWS),
new CumulusChannel("002",
        "Taiwan Formosa Live News",
        "https://www.youtube.com/embed/XxJKnDLYZz4?autoplay=1",
        "https://i.ytimg.com/vi/XxJKnDLYZz4/maxresdefault_live.jpg", "",
        TvContract.Programs.Genres.NEWS),*/
/*
        new CumulusChannel("900", "Euronews De", "http://fr-par-iphone-2.cdn.hexaglobe.net/streaming/euronews_ewns/14-live.m3u8", ""),
        new CumulusChannel("901", "TVI (Portugal)", "http://noscdn1.connectedviews.com:1935/live/smil:tvi.smil/playlist.m3u8", ""),
        new CumulusChannel("902", "PHOENIXHD", "http://teleboy.customers.cdn.iptv.ch/1122/index.m3u8", ""),
        new CumulusChannel("903", "Sport 1 Germany", "http://streaming-hub.com/tv/i/sport1_1@97464/index_1300_av-p.m3u8?sd=10&rebase=on", ""),
        new CumulusChannel("904", "RTP International", "http://rtp-pull-live.hls.adaptive.level3.net/liverepeater/rtpi_5ch120h264.stream/livestream.m3u8", "")
*/
