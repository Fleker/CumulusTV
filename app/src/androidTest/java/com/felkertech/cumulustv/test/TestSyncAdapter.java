package com.felkertech.cumulustv.test;

import android.content.Context;

import com.felkertech.channelsurfer.service.TvInputProvider;
import com.felkertech.channelsurfer.sync.SyncAdapter;

import junit.framework.Assert;

/**
 * Created by Nick on 9/3/2016.
 */

public class TestSyncAdapter extends SyncAdapter {
    public static TvInputProvider mTvInputProvider;

    public TestSyncAdapter(Context context) {
        super(context, false, false);
    }

    @Override
    public void doLocalSync() {
        final String inputId = TestTvProvider.INPUT_ID;
        Assert.assertNotNull("Input id is null", inputId);
        Assert.assertNotNull("You must set the static TvInputProvider!", mTvInputProvider);
        mTvInputProvider.performCustomSync(this, inputId);
    }
}
