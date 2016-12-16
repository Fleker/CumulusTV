package com.felkertech.cumulustv.test;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.model.Program;
import com.felkertech.channelsurfer.service.MultimediaInputProvider;

import junit.framework.Assert;
import junit.framework.Test;

import org.json.JSONException;

import java.util.List;

/**
 * Created by Nick on 9/3/2016.
 */

public class TestTvProvider extends MultimediaInputProvider {
    public static final String INPUT_ID = new ComponentName(
            TestTvProvider.class.getPackage().getName(), "." + TestTvProvider.class.getSimpleName())
            .flattenToString();

    public static TestFramework mTestFramework;

    @Override
    public View onCreateVideoView() {
        return null;
    }

    @Override
    public List<Channel> getAllChannels(Context context) {
        try {
            return VolatileChannelDatabase.getMockedInstance(context).getChannels();
        } catch (JSONException e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }

    @Override
    public List<Program> getProgramsForChannel(Context context, Uri channelUri, Channel channelInfo,
            long startTimeMs, long endTimeMs) {
        Assert.assertNotNull("You must set the public test framework!", mTestFramework);
        return mTestFramework.getProgramsForChannel(context, channelUri, channelInfo, startTimeMs,
                endTimeMs);
    }

    @Override
    public boolean onTune(Channel channel) {
        return false;
    }

    public interface TestFramework {
        List<Program> getProgramsForChannel(Context context, Uri channelUri, Channel channelInfo,
                long startTimeMs, long endTimeMs);
    }
}
