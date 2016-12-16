package com.felkertech.cumulustv.test;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;

import com.felkertech.cumulustv.tv.activities.LeanbackActivity;
import com.felkertech.cumulustv.tv.fragments.LeanbackFragment;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests common activities in the {@link LeanbackActivity} and {@link LeanbackFragment}.
 * This has to be a UI test, not an integration test.
 *
 * @author Nick
 * @version 2016.09.06
 */
@RunWith(AndroidJUnit4.class)
public class LeanbackActivityIntegrationTest {
    private UiDevice mDevice;

    public LeanbackActivityIntegrationTest() {
    }

    @Before
    public void initializeActivity() {
        Context context = InstrumentationRegistry.getContext();
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        // Start from the home screen
        mDevice.pressHome();
        final Intent intent = context.getPackageManager()
                .getLeanbackLaunchIntentForPackage("com.felkertech.n.cumulustv");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        mDevice.wait(Until.hasObject(By.pkg("com.felkertech.n.cumulustv").depth(0)),
                5000);
    }

    /**
     * Just open the activity and make sure it doesn't crash
     */
    @Test
    public void testExistenceofActivity() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(10, TimeUnit.SECONDS);
        Assert.assertNotNull(LeanbackActivity.lbf);
        Assert.assertNotNull(LeanbackActivity.lbf.getActivity());
    }
}
