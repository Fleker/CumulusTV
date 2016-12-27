package com.felkertech.cumulustv.test;

import android.os.Build;

import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.n.cumulustv.BuildConfig;
import com.felkertech.cumulustv.model.JsonChannel;
import com.google.android.media.tv.companionlibrary.model.Channel;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests various components of the {@link JsonChannel} class related to creation and parsing.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.M)
public class JsonChannelUnitTest extends TestCase {
    private static final boolean AUDIO_ONLY = true;
    private static final String EPG_URL = "http://example.com/program_guide.xml";
    private static final String GENRES = "ENTERTAINMENT";
    private static final String LOGO = "http://example.com/logo.png";
    private static final String NAME = "My Channel";
    private static final String MEDIA_URL = "http://example.com/stream.m3u8";
    private static final String NUMBER = "1-1";
    private static final String SPLASHSCREEN = "http://example.com/poster.png";

    public JsonChannelUnitTest() {}

    /**
     * Tests creating a CumulusChannel with the Builder class to make sure the builder works correctly.
     */
    @Test
    public void testBuilder() {
        CumulusChannel channel = new JsonChannel.Builder()
                .setAudioOnly(AUDIO_ONLY)
                .setEpgUrl(EPG_URL)
                .setGenres(GENRES)
                .setLogo(LOGO)
                .setMediaUrl(MEDIA_URL)
                .setName(NAME)
                .setNumber(NUMBER)
                .setPluginSource("")
                .setSplashscreen(SPLASHSCREEN)
                .build();
        assertEquals(AUDIO_ONLY, channel.isAudioOnly());
        assertEquals(EPG_URL, channel.getEpgUrl());
        assertEquals(GENRES, channel.getGenresString());
        assertEquals(LOGO, channel.getLogo());
        assertEquals(NAME, channel.getName());
        assertEquals(NUMBER, channel.getNumber());
        assertEquals(MEDIA_URL, channel.getMediaUrl());
        assertEquals(SPLASHSCREEN, channel.getSplashscreen());
    }

    /**
     * Tests creating a CumulusChannel directly from a {@link org.json.JSONObject}.
     */
    @Test
    public void testBuildFromJson() throws JSONException {
        CumulusChannel channel = new JsonChannel.Builder()
                .setAudioOnly(AUDIO_ONLY)
                .setEpgUrl(EPG_URL)
                .setGenres(GENRES)
                .setLogo(LOGO)
                .setMediaUrl(MEDIA_URL)
                .setName(NAME)
                .setNumber(NUMBER)
                .setSplashscreen(SPLASHSCREEN)
                .build();
        JSONObject jsonObject = channel.toJson();
        JsonChannel clonedChannel = new JsonChannel.Builder(jsonObject)
                .build();
        assertEquals(channel, clonedChannel);
        assertEquals(channel.toString(), clonedChannel.toString());
    }

    /**
     * Tests that a {@link JsonChannel} cannot be created without certain attributes
     */
    @Test
    public void testBuilderExceptions() {
        try {
            CumulusChannel channel = new JsonChannel.Builder()
                    .setName(NAME)
                    .setNumber(NUMBER)
                    .build();
            fail("There is no media url");
        } catch (IllegalArgumentException ignored) {
            // Exception correctly handled
        }
        try {
            CumulusChannel channel = new JsonChannel.Builder()
                    .setName(NAME)
                    .setMediaUrl(MEDIA_URL)
                    .build();
            fail("There is no number");
        } catch (IllegalArgumentException ignored) {
            // Exception correctly handled
        }
        try {
            CumulusChannel channel = new JsonChannel.Builder()
                    .setMediaUrl(MEDIA_URL)
                    .setNumber(NUMBER)
                    .build();
            fail("There is no name");
        } catch (IllegalArgumentException ignored) {
            // Exception correctly handled
        }
    }

    /**
     * Tests that we can use the .toChannel method to successfully create a channel object.
     */
    @Test
    public void testSuccessfulChannelConversion() {
        JsonChannel jsonChannel = new JsonChannel.Builder()
                .setAudioOnly(AUDIO_ONLY)
                .setEpgUrl(EPG_URL)
                .setGenres(GENRES)
                .setLogo(LOGO)
                .setMediaUrl(MEDIA_URL)
                .setName(NAME)
                .setNumber(NUMBER)
                .setSplashscreen(SPLASHSCREEN)
                .build();
        Channel channel = jsonChannel.toChannel();
        assertEquals(channel.getDisplayName(), jsonChannel.getName());
    }

    /**
     * Tests that we can cloneInto a CumulusChannel through the Builder to modify later.
     */
    @Test
    public void testJsonChannelCloning() {
        CumulusChannel jsonChannel = new JsonChannel.Builder()
                .setAudioOnly(AUDIO_ONLY)
                .setEpgUrl(EPG_URL)
                .setGenres(GENRES)
                .setLogo(LOGO)
                .setMediaUrl(MEDIA_URL)
                .setName(NAME)
                .setNumber(NUMBER)
                .setSplashscreen(SPLASHSCREEN)
                .build();
        JsonChannel clonedChannel = new JsonChannel.Builder(jsonChannel).build();
        assertEquals(clonedChannel, jsonChannel);
        CumulusChannel clonedChannel2 = new JsonChannel.Builder(clonedChannel)
                .setAudioOnly(!AUDIO_ONLY)
                .build();
        assertNotSame(clonedChannel2, clonedChannel);
        assertEquals(clonedChannel.getName(), clonedChannel2.getName());
    }
}
