package com.felkertech.cumulustv.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A type of data structure that simply links to a Url. This Url can be an M3U playlist which is
 * updated every time the app syncs.
 */
public class JsonListing {
    private static final String KEY_TYPE = "type";
    private static final String TYPE_JSON_LISTING = "jsonlisting";
    private static final String KEY_URL = "url";

    private String url;

    private JsonListing() {
    }

    public String getUrl() {
        return url;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(KEY_TYPE, TYPE_JSON_LISTING);
        object.put(KEY_URL, url);
        return object;
    }

    public String toString() {
        try {
            return toJson().toString();
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static class Builder {
        private JsonListing mJsonListing;

        public Builder() {
            mJsonListing = new JsonListing();
        }

        public Builder(JsonListing listing) {
            mJsonListing.url = listing.url;
        }

        public void setUrl(String url) {
            mJsonListing.url = url;
        }

        public JsonListing build() {
            if (mJsonListing.url == null || mJsonListing.url.isEmpty()) {
                throw new IllegalArgumentException("Url cannot be null");
            }
            return mJsonListing;
        }
    }
}
