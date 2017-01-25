package com.felkertech.cumulustv.test;

import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.felkertech.cumulustv.activities.MainActivity;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.fileio.XmlTvParser;
import com.felkertech.cumulustv.services.CumulusJobService;
import com.felkertech.n.cumulustv.R;
import com.google.android.media.tv.companionlibrary.EpgSyncJobService;
import com.google.android.media.tv.companionlibrary.model.Program;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Tests the ability to pull program data out of an Xmltv file and correctly insert it into the
 * program guide database.
 *
 * @author Nick
 * @version 2016-09-03
 */
@RunWith(AndroidJUnit4.class)
public class EpgSyncTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private static final String TAG = EpgSyncTest.class.getSimpleName();
    private static final String MEDIA_URL = "http://example.com/video.mp4";

    public EpgSyncTest() {
        super(MainActivity.class);
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        getActivity();
    }

    /**
     * From a resource file, we will try to pull EPG data for a channel.
     */
    @Test
    public void testPullFromXmlTvResource() throws JSONException, InterruptedException {
        VolatileChannelDatabase.getMockedInstance(getActivity()).add(
                new JsonChannel.Builder()
                        .setName("Name")
                        .setNumber("Number")
                        .setMediaUrl(MEDIA_URL)
                        .setEpgUrl("http://example.com/epg.xml")
                        .setLogo("http://static-cdn1.ustream.tv/i/channel/picture/9/4/0/8/9408562/9408562_iss_hr_1330361780,256x144,r:1.jpg")
                        .build());
        CumulusJobService.requestImmediateSync1(getActivity(),
                "com.felkertech.cumulustv.tv.CumulusTvTifService", CumulusJobService.DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS,
                new ComponentName(getActivity(), CumulusJobService.class));
        // Wait for sync to complete
        Thread.sleep(1000 * 10);
        ChannelDatabase channelDatabase = VolatileChannelDatabase.getMockedInstance(getActivity());

        // Get our channel row
        Thread.sleep(1000 * 5);
        HashMap<String, Long> databaseRowMap = channelDatabase.getHashMap();
        assertTrue(databaseRowMap.containsKey(MEDIA_URL));
        long rowId = databaseRowMap.get(MEDIA_URL);
        assertTrue(rowId > 0);

        // Get programs
        Cursor cursor = getActivity().getContentResolver().query(
                TvContract.buildProgramsUriForChannel(rowId), null, null, null, null);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        Program program = Program.fromCursor(cursor);
        if (!(program.getTitle().equals("Elephants Dream") ||
                program.getTitle().equals("Sintel"))) {
            Assert.fail("Neither program was found.");
        }
    }

    @After
    public void erase() {
        getActivity().getContentResolver().delete(TvContract.Channels.CONTENT_URI, null, null);
    }
}
