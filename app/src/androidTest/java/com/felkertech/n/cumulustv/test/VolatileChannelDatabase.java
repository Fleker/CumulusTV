package com.felkertech.n.cumulustv.test;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;

import com.felkertech.n.ActivityUtils;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JsonChannel;

import junit.framework.Assert;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;

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
        mMockChannelDatabase.initializeHashMap(context);
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

    @Override
    protected void initializeHashMap(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentResolver contentResolver = context.getContentResolver();
                Uri channelsUri = TvContract.buildChannelsUriForInput(
                        TestTvProvider.INPUT_ID);
                Cursor cursor = contentResolver.query(channelsUri, null, null, null, null);
                mDatabaseHashMap = new HashMap<>();
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String mediaUrl = cursor.getString(cursor.getColumnIndex(
                                TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA));
                        long rowId = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID));
                        try {
                            for (JsonChannel jsonChannel : getJsonChannels()) {
                                if (jsonChannel.getMediaUrl().equals(mediaUrl)) {
                                    mDatabaseHashMap.put(jsonChannel.getMediaUrl(), rowId);
                                }
                            }
                        } catch (JSONException e) {
                            Assert.fail(e.getMessage());
                        }
                    }
                    cursor.close();
                }
            }
        }).start();
    }
}
