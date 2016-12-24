package com.felkertech.cumulustv.fileio;

import android.graphics.Color;
import android.media.tv.TvContentRating;
import android.text.TextUtils;
import android.util.Xml;

import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.model.Program;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * XMLTV document parser which conforms to http://wiki.xmltv.org/index.php/Main_Page
 * <p>
 * <p>Please note that xmltv.dtd are extended to be align with Android TV Input Framework and
 * contain static video contents:
 * <p>
 * <!ELEMENT channel ([elements in xmltv.dtd], display-number, app-link) > <!ATTLIST channel
 * [attributes in xmltv.dtd] repeat-programs CDATA #IMPLIED > <!ATTLIST programme [attributes in
 * xmltv.dtd] video-src CDATA #IMPLIED video-type CDATA #IMPLIED > <!ELEMENT app-link (icon) >
 * <!ATTLIST app-link text CDATA #IMPLIED color CDATA #IMPLIED poster-uri CDATA #IMPLIED intent-uri
 * CDATA #IMPLIED >
 * <p>
 * display-number : The channel number that is displayed to the user. repeat-programs : If "true",
 * the programs in the xml document are scheduled sequentially in a loop regardless of their start
 * and end time. This is introduced to simulate a live channel in this sample. video-src : The video
 * URL for the given program. This can be omitted if the xml will be used only for the program guide
 * update. video-type : The video type. Should be one of "HTTP_PROGRESSIVE", "HLS", and "MPEG-DASH".
 * This can be omitted if the xml will be used only for the program guide update. app-link : The
 * app-link allows channel input sources to provide activity links from their live channel
 * programming to another activity. This enables content providers to increase user engagement by
 * offering the viewer other content or actions. text : The text of the app link template for this
 * channel. color : The accent color of the app link template for this channel. This is primarily
 * used for the background color of the text box in the template. poster-uri : The URI for the
 * poster art used as the background of the app link template for this channel. intent-uri : The
 * intent URI of the app link for this channel. It should be created using Intent.toUri(int) with
 * Intent.URI_INTENT_SCHEME. (see https://developer.android
 * .com/reference/android/media/tv/TvContract.Channels.html#COLUMN_APP_LINK_INTENT_URI)
 * The intent is launched when the user clicks the corresponding app link for the current channel.
 */
@Deprecated
public class XmlTvParser {
    private static final String TAG_TV = "tv";
    private static final String TAG_CHANNEL = "channel";
    private static final String TAG_DISPLAY_NAME = "display-name";
    private static final String TAG_ICON = "icon";
    private static final String TAG_APP_LINK = "app-link";
    private static final String TAG_PROGRAM = "programme";
    private static final String TAG_TITLE = "title";
    private static final String TAG_DESC = "desc";
    private static final String TAG_CATEGORY = "category";
    private static final String TAG_RATING = "rating";
    private static final String TAG_VALUE = "value";
    private static final String TAG_DISPLAY_NUMBER = "display-number";

    private static final String ATTR_ID = "id";
    private static final String ATTR_START = "start";
    private static final String ATTR_STOP = "stop";
    private static final String ATTR_CHANNEL = "channel";
    private static final String ATTR_SYSTEM = "system";
    private static final String ATTR_SRC = "src";
    private static final String ATTR_REPEAT_PROGRAMS = "repeat-programs";
    private static final String ATTR_VIDEO_SRC = "video-src";
    private static final String ATTR_VIDEO_TYPE = "video-type";
    private static final String ATTR_APP_LINK_TEXT = "text";
    private static final String ATTR_APP_LINK_COLOR = "color";
    private static final String ATTR_APP_LINK_POSTER_URI = "poster-uri";
    private static final String ATTR_APP_LINK_INTENT_URI = "intent-uri";

    private static final String VALUE_VIDEO_TYPE_HTTP_PROGRESSIVE = "HTTP_PROGRESSIVE";
    private static final String VALUE_VIDEO_TYPE_HLS = "HLS";
    private static final String VALUE_VIDEO_TYPE_MPEG_DASH = "MPEG_DASH";

    private static final String ANDROID_TV_RATING = "com.android.tv";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss Z",
            Locale.US);

    private XmlTvParser() {
    }

    public static TvListing parse(InputStream inputStream) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream, null);
            int eventType = parser.next();
            if (eventType != XmlPullParser.START_TAG || !TAG_TV.equals(parser.getName())) {
                throw new RuntimeException("inputStream does not contain a xml tv description");
            }
            return parseTvListings(parser);
        } catch (XmlPullParserException | IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static TvListing parseTvListings(XmlPullParser parser)
            throws IOException, XmlPullParserException, ParseException {
        List<Channel> channels = new ArrayList<>();
        List<Program> programs = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG
                    && TAG_CHANNEL.equalsIgnoreCase(parser.getName())) {
                channels.add(parseChannel(parser));
            }
            if (parser.getEventType() == XmlPullParser.START_TAG
                    && TAG_PROGRAM.equalsIgnoreCase(parser.getName())) {
                programs.add(parseProgram(parser));
            }
        }
        return new TvListing(channels, programs);
    }

    private static Channel parseChannel(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        String id = null;
        for (int i = 0; i < parser.getAttributeCount(); ++i) {
            String attr = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (ATTR_ID.equalsIgnoreCase(attr)) {
                id = value;
            }
        }
        String displayName = null;
        String displayNumber = null;
        XmlTvIcon icon = null;
        XmlTvAppLink appLink = null;
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if (TAG_DISPLAY_NAME.equalsIgnoreCase(parser.getName())
                        && displayName == null) {
                    // TODO: support multiple display names.
                    displayName = parser.nextText();
                } else if (TAG_DISPLAY_NUMBER.equalsIgnoreCase(parser.getName())
                        && displayNumber == null) {
                    displayNumber = parser.nextText();
                } else if (TAG_ICON.equalsIgnoreCase(parser.getName()) && icon == null) {
                    icon = parseIcon(parser);
                } else if (TAG_APP_LINK.equalsIgnoreCase(parser.getName()) && appLink == null) {
                    appLink = parseAppLink(parser);
                }
            } else if (TAG_CHANNEL.equalsIgnoreCase(parser.getName())
                    && parser.getEventType() == XmlPullParser.END_TAG) {
                break;
            }
        }
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(displayName)) {
            throw new IllegalArgumentException("id and display-name can not be null.");
        }

        int fakeOriginalNetworkId = Objects.hash(displayName, displayNumber);
        return new Channel()
                .setName(displayName)
                .setNumber(displayNumber)
//                .setLogoUrl((icon == null) ? null : icon.src)
//                .setAppLinkIcon((appLink == null || appLink.icon == null) ? null : appLink.icon.src)
//                .setAppLinkColor((appLink == null) ? null : appLink.color)
//                .setAppLinkText((appLink == null) ? null : appLink.text)
//                .setAppLinkIntent((appLink == null) ? null : appLink.intentUri)
                .setOriginalNetworkId(fakeOriginalNetworkId);
//                .setAppLinkPoster((appLink == null) ? null : appLink.posterUri);
    }

    private static Program parseProgram(XmlPullParser parser)
            throws IOException, XmlPullParserException, ParseException {
        String channelId = null;
        Long startTimeUtcMillis = null;
        Long endTimeUtcMillis = null;
        String videoSrc = null;
        int videoType = 1;
        for (int i = 0; i < parser.getAttributeCount(); ++i) {
            String attr = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (ATTR_CHANNEL.equalsIgnoreCase(attr)) {
                channelId = value;
            } else if (ATTR_START.equalsIgnoreCase(attr)) {
                startTimeUtcMillis = DATE_FORMAT.parse(value).getTime();
            } else if (ATTR_STOP.equalsIgnoreCase(attr)) {
                endTimeUtcMillis = DATE_FORMAT.parse(value).getTime();
            } else if (ATTR_VIDEO_SRC.equalsIgnoreCase(attr)) {
                videoSrc = value;
            } else if (ATTR_VIDEO_TYPE.equalsIgnoreCase(attr)) {
                // TODO Set to constants
                if (VALUE_VIDEO_TYPE_HTTP_PROGRESSIVE.equals(value)) {
                    videoType = 1;
                } else if (VALUE_VIDEO_TYPE_HLS.equals(value)) {
                    videoType = 2;
                } else if (VALUE_VIDEO_TYPE_MPEG_DASH.equals(value)) {
                    videoType = 3;
                }
            }
        }
        String title = null;
        String description = null;
        XmlTvIcon icon = null;
        List<String> category = new ArrayList<>();
        List<TvContentRating> rating = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if (TAG_TITLE.equalsIgnoreCase(parser.getName())) {
                    title = parser.nextText();
                } else if (TAG_DESC.equalsIgnoreCase(tagName)) {
                    description = parser.nextText();
                } else if (TAG_ICON.equalsIgnoreCase(tagName)) {
                    icon = parseIcon(parser);
                } else if (TAG_CATEGORY.equalsIgnoreCase(tagName)) {
                    category.add(parser.nextText());
                } else if (TAG_RATING.equalsIgnoreCase(tagName)) {
                    rating.add(parseRating(parser));
                }
            } else if (TAG_PROGRAM.equalsIgnoreCase(tagName)
                    && parser.getEventType() == XmlPullParser.END_TAG) {
                break;
            }
        }
        if (TextUtils.isEmpty(channelId) || startTimeUtcMillis == null
                || endTimeUtcMillis == null) {
            throw new IllegalArgumentException("channel, start, and end can not be null.");
        }
        return new Program.Builder()
                .setTitle(title)
                .setDescription(description)
                .setPosterArtUri(icon.src)
                .setStartTimeUtcMillis(startTimeUtcMillis)
                .setEndTimeUtcMillis(endTimeUtcMillis)
                .setInternalProviderData(videoSrc)
                .setContentRatings(rating.toArray(new TvContentRating[rating.size()]))
                .setCanonicalGenres(category.toArray(new String[category.size()]))
                .build();
    }

    private static XmlTvIcon parseIcon(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        String src = null;
        for (int i = 0; i < parser.getAttributeCount(); ++i) {
            String attr = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (ATTR_SRC.equalsIgnoreCase(attr)) {
                src = value;
            }
        }
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (TAG_ICON.equalsIgnoreCase(parser.getName())
                    && parser.getEventType() == XmlPullParser.END_TAG) {
                break;
            }
        }
        if (TextUtils.isEmpty(src)) {
            throw new IllegalArgumentException("src cannot be null.");
        }
        return new XmlTvIcon(src);
    }

    private static XmlTvAppLink parseAppLink(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        String text = null;
        Integer color = null;
        String posterUri = null;
        String intentUri = null;
        for (int i = 0; i < parser.getAttributeCount(); ++i) {
            String attr = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (ATTR_APP_LINK_TEXT.equalsIgnoreCase(attr)) {
                text = value;
            } else if (ATTR_APP_LINK_COLOR.equalsIgnoreCase(attr)) {
                color = Color.parseColor(value);
            } else if (ATTR_APP_LINK_POSTER_URI.equalsIgnoreCase(attr)) {
                posterUri = value;
            } else if (ATTR_APP_LINK_INTENT_URI.equalsIgnoreCase(attr)) {
                intentUri = value;
            }
        }

        XmlTvIcon icon = null;
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG
                    && TAG_ICON.equalsIgnoreCase(parser.getName()) && icon == null) {
                icon = parseIcon(parser);
            } else if (TAG_APP_LINK.equalsIgnoreCase(parser.getName())
                    && parser.getEventType() == XmlPullParser.END_TAG) {
                break;
            }
        }

        return new XmlTvAppLink(text, color, posterUri, intentUri, icon);
    }

    private static TvContentRating parseRating(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        String system = null;
        for (int i = 0; i < parser.getAttributeCount(); ++i) {
            String attr = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (ATTR_SYSTEM.equalsIgnoreCase(attr)) {
                system = value;
            }
        }
        String value = null;
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if (TAG_VALUE.equalsIgnoreCase(parser.getName())) {
                    value = parser.nextText();
                }
            } else if (TAG_RATING.equalsIgnoreCase(parser.getName())
                    && parser.getEventType() == XmlPullParser.END_TAG) {
                break;
            }
        }
        if (TextUtils.isEmpty(system) || TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException("system and value cannot be null.");
        }
        return TvContentRating.createRating("com.android.tv", system, value);
    }

    public static class TvListing implements ITvListing, SingleChannel {
        private final List<Channel> channels;
        private final List<Program> programs;

        private TvListing(List<Channel> channels, List<Program> programs) {
            this.channels = channels;
            this.programs = programs;
        }

        @Override
        public List<Channel> getChannels() {
            return channels;
        }

        @Override
        public List<Program> getProgramsFor(Channel channel) {
            List<Program> programs = new ArrayList<>();
            for (Program program : programs) {
                if (program.getChannelId() == channel.getChannelId()) {
                    programs.add(program);
                }
            }
            return programs;
        }

        @Override
        public List<Program> getAllPrograms() {
            return programs;
        }
    }

    @Deprecated
    public static class XmlTvIcon {
        public final String src;

        private XmlTvIcon(String src) {
            this.src = src;
        }
    }

    @Deprecated
    public static class XmlTvAppLink {
        public final String text;
        public final Integer color;
        public final String posterUri;
        public final String intentUri;
        public final XmlTvIcon icon;

        public XmlTvAppLink(String text, Integer color, String posterUri, String intentUri,
                            XmlTvIcon icon) {
            this.text = text;
            this.color = color;
            this.posterUri = posterUri;
            this.intentUri = intentUri;
            this.icon = icon;
        }
    }
}