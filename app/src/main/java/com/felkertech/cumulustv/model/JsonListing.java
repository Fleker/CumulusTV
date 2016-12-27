package com.felkertech.cumulustv.model;

import com.felkertech.cumulustv.plugins.JsonContainer;

import org.json.JSONException;
import org.json.JSONObject;

import static com.felkertech.cumulustv.model.ChannelDatabaseFactory.KEY_TYPE;
import static com.felkertech.cumulustv.model.ChannelDatabaseFactory.TYPE_JSON_LISTING;

/**
 * A type of data structure that simply links to a Url. This Url can be an M3U playlist which is
 * updated every time the app syncs.
 */
public class JsonListing implements JsonContainer {
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

        public Builder(JSONObject object) {
            try {
                mJsonListing.url = object.getString(KEY_URL);
            } catch (JSONException ignored) {
            }
        }

        public Builder setUrl(String url) {
            mJsonListing.url = url;
            return this;
        }

        public JsonListing build() {
            if (mJsonListing.url == null || mJsonListing.url.isEmpty()) {
                throw new IllegalArgumentException("Url cannot be null");
            }
            return mJsonListing;
        }
    }
}
