package com.felkertech.cumulustv.test;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;

import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.google.android.media.tv.companionlibrary.model.Channel;

import junit.framework.Assert;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Provides a test infrastructure for a {@link ChannelDatabase} where everything exists only in
 * memory. Nothing is persistent.
 * @author Nick
 */
public class VolatileChannelDatabase extends ChannelDatabase {
    public static final String KEY = VolatileChannelDatabase.class.getCanonicalName();

    private static VolatileChannelDatabase mMockChannelDatabase;
    private static ArrayList<CumulusChannel> mJsonChannels = new ArrayList<>();

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
    public void add(CumulusChannel channel) throws JSONException {
        mJsonChannels.add(channel);
    }

    @Override
    public void delete(CumulusChannel channel) throws JSONException {
        mJsonChannels.remove(channel);
    }

    @Override
    public List<Channel> getChannels() throws JSONException {
        List<Channel> channelList = new ArrayList<>();
        for (int i = 0; i < mJsonChannels.size(); i++) {
            JsonChannel jsonChannel = (JsonChannel) mJsonChannels.get(i);
            Channel channel = jsonChannel.toChannel();
            channelList.add(channel);
        }
        return channelList;
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
                        "com.felkertech.cumulustv.tv.CumulusTvTifService");
                Cursor cursor = contentResolver.query(channelsUri, null, null, null, null);
                mDatabaseHashMap = new HashMap<>();
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String mediaUrl = cursor.getString(cursor.getColumnIndex(
                                TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA));
                        long rowId = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID));
                        try {
                            for (CumulusChannel jsonChannel : mJsonChannels) {
                                if (jsonChannel.getMediaUrl().equals(mediaUrl)) {
                                    mDatabaseHashMap.put(jsonChannel.getMediaUrl(), rowId);
                                }
                            }
                        } catch (Exception e) {
                            Assert.fail(e.getMessage());
                        }
                    }
                    cursor.close();
                }
            }
        }).start();
    }
}
