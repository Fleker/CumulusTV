package com.felkertech.n.cumulustv.xmltv;

import android.content.ContentResolver;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by guest1 on 8/25/2015.
 * Modeled from https://code.google.com/r/wwelles-xmltv/source/browse/src/ru/butt/xmltv/model/XMLTVParser.java
 */
public class XMLTVParser {
    // We don't use namespaces
    private static final String ns = null;
    private static String TAG = "cumulus:XMLTVParser";
    private static final String TV = "tv";
    private static final String PROGRAM = "programme";
    private static final String CHANNEL = "channel";

    public static List<Program> parse(String in) throws XmlPullParserException, IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            Log.d(TAG, "Start parsing");
            Log.d(TAG, in.substring(0, 36));
            xpp.setInput(new StringReader(in));
            int eventType = xpp.getEventType();
            Log.d(TAG, eventType+"");
//            if(eventType == XmlPullParser.START_DOCUMENT)
//                xpp.next();
            /*
            xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xpp.setInput(new InputStreamReader(in));*/
            /*xpp.nextTag();
            xpp.nextTag();
            */return readFeed(xpp);
        } finally {
//            in.close();
        }
    }
    private static List<Program> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Program> entries = new ArrayList<>();

//        parser.require(XmlPullParser.START_TAG, ns, "tv");
        Log.d(TAG, parser.getName()+"");
        int eventType = parser.getEventType();
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            eventType = parser.getEventType();
            if (eventType == XmlPullParser.START_DOCUMENT) {
                Log.d(TAG, "Start document");
//                parser.next();
//                continue;
            } else if (eventType == XmlPullParser.END_DOCUMENT) {
                Log.d(TAG, "Doc ended");
//                continue;
            } else if (eventType == XmlPullParser.START_TAG) {
                Log.d(TAG, "Found tag start");
                String name = parser.getName();
                Log.d(TAG, name+"<");
                if(name.equals("tv")) {
                    entries.addAll(createTV(parser));
                    return entries;
                }
                if (name.equals("channel")) {
//                    createChannel(contentResolver, xpp);
                } else if (name.equals("programme")) {
//                    entries.add(createProgramme(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                Log.d(TAG, "Tag ended");
//                continue;
            } else if (eventType == XmlPullParser.TEXT) {
                Log.d(TAG, "Found text "+parser.getText());
//                continue;
            }
//            eventType = parser.next();
            Log.d(TAG, "parse " + parser.getLineNumber() + " line "+eventType+"   "+ parser.getText());
        }
        Log.d(TAG,"Finish parsing");
        return entries;
    }
    private static List<Program> createTV(XmlPullParser xpp) throws IOException, XmlPullParserException {
        Log.d(TAG, "Created TV parsings");
        List<Program> entries = new ArrayList<>();
        int eventType = xpp.next();
        String n = "";
        if(xpp.getName() != null)
            n = xpp.getName();
        while (!(eventType == XmlPullParser.END_TAG && n.equals(TV))) {
            if (eventType == XmlPullParser.START_TAG) {
//                Log.d(TAG, "Found tag '"+xpp.getName()+"'");
                if(xpp.getName().equals(PROGRAM)) {
                    entries.add(createProgramme(xpp));
                }
            }
            eventType = xpp.next();
            if(xpp.getName() != null)
                n = xpp.getName();
        }
        return entries;
    }
    private static Program createProgramme(XmlPullParser xpp) throws IOException, XmlPullParserException {
        Log.d(TAG, "Found program");
        String channelid = xpp.getAttributeValue(null, "channel");
        long start = getTime(xpp.getAttributeValue(null, "start")).getTime();
        long end = getTime(xpp.getAttributeValue(null, "stop")).getTime();
        Program p = new Program(channelid, start, end);

        int eventType = xpp.next();
        while (eventType != XmlPullParser.END_TAG && !"programme".equals(xpp.getName())) {
            if (eventType == XmlPullParser.START_TAG) {
                if("title".equals(xpp.getName())) {
                    String pr = xpp.nextText();
                    Log.d(TAG, "Added program "+pr);
                    p.setTitle(pr);
                } else if("desc".equals(xpp.getName())) {
                    p.setDescription(xpp.nextText());
                } else if("category".equals(xpp.getName())) {
//                    contentValues.put(XmlTvDefs.ProgrammeColumns.CATEGORY_ID, getCategoryId(contentResolver,xpp.nextText()));
                } else if("date".equals(xpp.getName())) {
                    p.setAirDate(getDate(xpp.nextText()).getTime());
                } else if("sub-title".equals(xpp.getName())) {
                    p.setSubtitle(xpp.nextText());
                }
            }
            eventType = xpp.next();
        }
        return p;
    }
    private static Date getTime(String start) {
        Calendar cal = new GregorianCalendar(Integer.parseInt(start.substring(0, 4)),
                Integer.parseInt(start.substring(4, 6)) - 1,
                Integer.parseInt(start.substring(6, 8)),
                Integer.parseInt(start.substring(8, 10)),
                Integer.parseInt(start.substring(10, 12)),
                Integer.parseInt(start.substring(12, 14)));
//        cal.setTimeZone(new ZoneInfo()); //TODO need include time zone info
        return cal.getTime();
    }
    private static Date getDate(String start) {
        Calendar cal = new GregorianCalendar(Integer.parseInt(start.substring(0, 4)),
                Integer.parseInt(start.substring(4, 6)) - 1,
                Integer.parseInt(start.substring(6, 8)));
//        cal.setTimeZone(new ZoneInfo()); //TODO need include time zone info
        return cal.getTime();
    }
}
