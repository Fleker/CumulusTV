package com.felkertech.cumulustv.test.tvs;

import android.app.Activity;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.cumulustv.tv.activities.LeanbackActivity;
import com.felkertech.cumulustv.utils.ActivityUtils;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *Tests creating and editing of channels
 */
@RunWith(AndroidJUnit4.class)
public class MainPickerIntegrationTest extends ActivityInstrumentationTestCase2<LeanbackActivity> {
    private static final String TAG = com.felkertech.cumulustv.test.TifDatabaseIntegrationTest.class.getSimpleName();
    private static final String MEDIA_URL = "http://example.com/stream.mp4";

    private Uri mDatabaseUri;

    public MainPickerIntegrationTest() {
        super(LeanbackActivity.class);
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        getActivity();
    }

    @Before
    public void insertChannels() throws JSONException {
        ChannelDatabase channelDatabase = ChannelDatabase.getInstance(getActivity());
        CumulusChannel channel = new CumulusChannel.Builder()
                .setName("Hello")
                .setNumber("12-3")
                .setMediaUrl(MEDIA_URL)
                .build();
        channelDatabase.add(channel);
    }

    @Test
    public void addChannel() throws InterruptedException {
        Activity activity = getActivity();
        ActivityUtils.openPluginPicker(true, activity);
        // Does it crash?
        CountDownLatch latch = new CountDownLatch(1);
        assertFalse(latch.await(2, TimeUnit.SECONDS));
/*        assertEquals("",
                ((TextView) MainPicker.streamView).getText());*/
        activity.finish();
    }

    @Test
    public void editChannel() throws InterruptedException {
        Activity activity = getActivity();
        ActivityUtils.editChannel(activity, MEDIA_URL);
        // Does it crash?
        CountDownLatch latch = new CountDownLatch(1);
        assertFalse(latch.await(2, TimeUnit.SECONDS));
/*        assertEquals(MEDIA_URL,
                ((TextView) MainPicker.streamView).getText());*/
        activity.finish();
    }

    @Test
    public void updateChannel() throws InterruptedException, JSONException {
        Activity activity = getActivity();
        ActivityUtils.editChannel(activity, MEDIA_URL);
        // Does it crash?
        CountDownLatch latch = new CountDownLatch(1);
        assertFalse(latch.await(2, TimeUnit.SECONDS));
        // Update ChannelDatabase
        ChannelDatabase channelDatabase = ChannelDatabase.getInstance(activity);
        CumulusChannel channel = channelDatabase.findChannelByMediaUrl(MEDIA_URL);
        channel = new CumulusChannel.Builder(channel)
                .setNumber("456")
                .build();
        channelDatabase.update(channel);
        // Check value
        assertEquals("456", channelDatabase.findChannelByMediaUrl(MEDIA_URL).getNumber());
        activity.finish();
    }

    @After
    public void deleteChannels() throws JSONException {
        ChannelDatabase channelDatabase = ChannelDatabase.getInstance(getActivity());
        CumulusChannel channel = channelDatabase.findChannelByMediaUrl(MEDIA_URL);
        channelDatabase.delete(channel);
    }
}