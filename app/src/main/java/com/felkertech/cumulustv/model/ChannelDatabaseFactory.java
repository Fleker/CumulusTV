package com.felkertech.cumulustv.model;

import android.util.Log;

import org.json.JSONObject;

/**
 * This class uses static methods to determine which type of object is being read and provides
 * useful methods for parsing
 */
public class ChannelDatabaseFactory {
    private static final String TAG = ChannelDatabaseFactory.class.getSimpleName();

    public static final String KEY_TYPE = "type";
    public static final String TYPE_JSON_LISTING = "jsonlisting";

    public static void parseType(JSONObject entry, ChannelParser parser) {
        try {
            if (!entry.has(KEY_TYPE)) {
                parser.ifJsonChannel(new JsonChannel.Builder(entry).build());
            } else if (entry.getString(KEY_TYPE).equals(TYPE_JSON_LISTING)) {
                parser.ifJsonListing(new JsonListing.Builder(entry).build());
            }
        } catch (Exception e) {
            Log.e(TAG, entry.toString());
            throw new IllegalArgumentException("Invalid JSON: " + entry.toString() + "    " +
                    e.getMessage());
        }
    }

    public interface ChannelParser {
        void ifJsonChannel(JsonChannel entry);
        void ifJsonListing(JsonListing entry);
    }
}
