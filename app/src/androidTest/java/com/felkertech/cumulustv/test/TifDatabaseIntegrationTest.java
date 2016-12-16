package com.felkertech.cumulustv.test;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.cumulustv.activities.MainActivity;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests integration with the Tv Input Framework.
 */
@RunWith(AndroidJUnit4.class)
public class TifDatabaseIntegrationTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private static final String TAG = TifDatabaseIntegrationTest.class.getSimpleName();

    private static final String MEDIA_URL = "http://example.com/stream.mp4";

    public TifDatabaseIntegrationTest() {
        super(MainActivity.class);
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        getActivity();
    }

    @Before
    public void insertChannels() {
        Context context = getActivity();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, 1);
        contentValues.put(TvContract.Channels.COLUMN_DISPLAY_NAME, "Hello");
        contentValues.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, "123");
        contentValues.put(TvContract.Channels.COLUMN_INPUT_ID,
                ActivityUtils.TV_INPUT_SERVICE.flattenToString());
        contentValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, MEDIA_URL);
        Uri databaseUri = context.getContentResolver().insert(TvContract.Channels.CONTENT_URI,
                contentValues);
        Log.d(TAG, "Inserted in Uri " + databaseUri);

        // Make sure we actually inserted something
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(TvContract.Channels.CONTENT_URI,
                null, null, null, null);
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToNext());
        assertEquals(1, cursor.getLong(cursor.getColumnIndex(
                TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID)));
        cursor.close();
    }

    /**
     * Test the ability to pull Channels from the database and link them to a {@link JsonChannel}.
     */
    @Test
    public void testChannelDatabase() throws JSONException, InterruptedException {
        ChannelDatabase channelDatabase =
                ChannelDatabase.getInstance(getActivity());
        JsonChannel jsonChannel = new JsonChannel.Builder()
                .setName("Hello")
                .setMediaUrl(MEDIA_URL)
                .setNumber("1234")
                .build();
        channelDatabase.add(jsonChannel);
        // Wait for HashMap to reload
        Thread.sleep(1000 * 5);
        assertEquals(1, channelDatabase.getHashMap().size());
        assertTrue(channelDatabase.getHashMap().containsKey(jsonChannel.getMediaUrl()));
        assertTrue(channelDatabase.getHashMap().get(jsonChannel.getMediaUrl()) > 0);
    }

    /**
     * Test generating a {@link JsonChannel} and inserting that correctly.
     */
    @Test
    public void testJsonChannelConverter() {
        JsonChannel jsonChannel = new JsonChannel.Builder()
                .setName("Hello")
                .setMediaUrl(MEDIA_URL)
                .setNumber("1234")
                .build();
        Channel channel = jsonChannel.toChannel();
        // This cannot be done until we update the Channel model.
    }

    @After
    public void deleteChannels() {
        Context context = getActivity();
        context.getContentResolver().delete(TvContract.Channels.CONTENT_URI, null, null);
    }
}