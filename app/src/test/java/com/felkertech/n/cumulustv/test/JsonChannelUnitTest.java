package com.felkertech.n.cumulustv.test;

import android.os.Build;

import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.n.cumulustv.BuildConfig;
import com.felkertech.n.cumulustv.model.JsonChannel;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests various components of the {@link JsonChannel} class related to creation and parsing.
 */
@RunWith(RobolectricGradleTestRunner.class)
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

    /**
     * Tests creating a JsonChannel with the Builder class to make sure the builder works correctly.
     */
    @Test
    public void testBuilder() {
        JsonChannel channel = new JsonChannel.Builder()
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
     * Tests creating a JsonChannel directly from a {@link org.json.JSONObject}.
     */
    @Test
    public void testBuildFromJson() throws JSONException {
        JsonChannel channel = new JsonChannel.Builder()
                .setAudioOnly(AUDIO_ONLY)
                .setEpgUrl(EPG_URL)
                .setGenres(GENRES)
                .setLogo(LOGO)
                .setMediaUrl(MEDIA_URL)
                .setName(NAME)
                .setNumber(NUMBER)
                .setSplashscreen(SPLASHSCREEN)
                .build();
        JSONObject jsonObject = channel.toJSON();
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
            JsonChannel channel = new JsonChannel.Builder()
                    .setName(NAME)
                    .setNumber(NUMBER)
                    .build();
            fail("There is no media url");
        } catch (IllegalArgumentException ignored) {
            // Exception correctly handled
        }
        try {
            JsonChannel channel = new JsonChannel.Builder()
                    .setName(NAME)
                    .setMediaUrl(MEDIA_URL)
                    .build();
            fail("There is no number");
        } catch (IllegalArgumentException ignored) {
            // Exception correctly handled
        }
        try {
            JsonChannel channel = new JsonChannel.Builder()
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
        assertEquals(channel.getName(), jsonChannel.getName());
    }

    /**
     * Tests that we can clone a JsonChannel through the Builder to modify later.
     */
    @Test
    public void testJsonChannelCloning() {
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
        JsonChannel clonedChannel = new JsonChannel.Builder(jsonChannel).build();
        assertEquals(clonedChannel, jsonChannel);
        JsonChannel clonedChannel2 = new JsonChannel.Builder(clonedChannel)
                .setAudioOnly(!AUDIO_ONLY)
                .build();
        assertNotSame(clonedChannel2, clonedChannel);
        assertEquals(clonedChannel.getName(), clonedChannel2.getName());
    }
}
