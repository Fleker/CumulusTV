package com.felkertech.n.cumulustv.test;

import android.content.Context;

import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JsonChannel;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Provides a test infrastructure for a {@link ChannelDatabase} where everything exists only in
 * memory. Nothing is persistent.
 * @author Nick
 */
public class VolatileChannelDatabase extends ChannelDatabase {
    public static final String KEY = VolatileChannelDatabase.class.getCanonicalName();

    private static VolatileChannelDatabase mMockChannelDatabase;
    private static ArrayList<JsonChannel> mJsonChannels = new ArrayList<>();

    public static VolatileChannelDatabase getMockedInstance(Context context) throws JSONException {
        if (mMockChannelDatabase == null) {
            mMockChannelDatabase = new VolatileChannelDatabase(context);
        }
        return mMockChannelDatabase;
    }

    public static void reset() {
        mMockChannelDatabase = null;
    }

    protected VolatileChannelDatabase(final Context context) throws JSONException {
        super(context);
    }

    @Override
    public void add(JsonChannel channel) throws JSONException {
        mJsonChannels.add(channel);
    }

    @Override
    public void delete(JsonChannel channel) throws JSONException {
        mJsonChannels.remove(channel);
    }

    @Override
    public void save() {
    }
}
