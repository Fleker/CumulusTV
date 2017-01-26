package com.felkertech.cumulustv.test;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.felkertech.cumulustv.fileio.M3uParser;
import com.felkertech.n.cumulustv.BuildConfig;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.Assert.assertEquals;

/**
 * Created by Nick on 12/15/2016.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
@RequiresApi(api = Build.VERSION_CODES.M)
public class M3uParserUnitTest {

    public M3uParserUnitTest() {
        super();
    }

    public InputStream openFile(String fileName) {
        return this.getClass().getClassLoader().getResourceAsStream(fileName);
    }

    @Test
    public void testFile1() throws IOException {
        M3uParser.TvListing listing = M3uParser.parse(openFile("m3u_test1.m3u"));
        assertEquals(2, listing.channels.size());
        assertEquals("Canal 2 HD", listing.channels.get(0).toJsonChannel().getName());
        assertEquals("http://y.cdn.entutele.com/media/channels/big/canal-2-hd.gif",
                listing.channels.get(0).toJsonChannel().getLogo());
        assertEquals("Foro TV HD", listing.channels.get(1).toJsonChannel().getName());
        assertEquals("http://y.cdn.entutele.com/media/channels/big/foro-tv-hd.gif",
                listing.channels.get(1).toJsonChannel().getLogo());
    }

    @Test
    public void testFile2() throws IOException {
        M3uParser.TvListing listing = M3uParser.parse(openFile("m3u_test2.m3u"));
        assertEquals(1, listing.channels.size());
        assertEquals("http://s.xxx.com:8000/live/xxx/xxx/1.ts", listing.channels.get(0).toJsonChannel().getMediaUrl());
    }

    @Test
    public void testFile3() throws IOException {
        M3uParser.TvListing listing = M3uParser.parse(openFile("m3u_test3.m3u"));
        assertEquals(1, listing.channels.size());
        assertEquals("http://s.xxx.com:8000/live/xxx/xxx/1.ts", listing.channels.get(0).toJsonChannel().getMediaUrl());
    }
}
