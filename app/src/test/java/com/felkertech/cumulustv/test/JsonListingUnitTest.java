package com.felkertech.cumulustv.test;

import android.os.Build;

import com.felkertech.cumulustv.model.JsonListing;
import com.felkertech.n.cumulustv.BuildConfig;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Verifies that a {@link JsonListing} works as expected.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.M)
public class JsonListingUnitTest extends TestCase {
    public static final String M3U_URL = "https://raw.githubusercontent.com/Fleker/CumulusTV/mas" +
            "ter/app/src/test/resources/m3u_test1.m3u";
    @Test
    public void testSuccessfulBuilder() {
        JsonListing listing = new JsonListing.Builder()
                .setUrl(M3U_URL)
                .build();
        assertEquals(M3U_URL, listing.getUrl());
    }

    @Test
    public void testUnsuccessfulBuilder() {
        try {
            JsonListing listing = new JsonListing.Builder().build();
            fail("This should not succeed");
        } catch (IllegalArgumentException e) {
            // Exception successfully thrown
        }
    }
}
