package com.felkertech.n.cumulustv.test.tvs;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by Nick on 11/4/2016.
 */

public class PickerIntegrationTest {
    private UiDevice mDevice;
    private Context mContext;

    public PickerIntegrationTest() {
    }

    @Before
    public void initializeActivity() {
        mContext = InstrumentationRegistry.getContext();
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        // Start from the home screen
        mDevice.pressHome();
        final Intent intent = mContext.getPackageManager()
                .getLeanbackLaunchIntentForPackage("com.felkertech.n.cumulustv");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);
        mDevice.wait(Until.hasObject(By.pkg("com.felkertech.n.cumulustv").depth(0)),
                5000);
    }

    /**
     * Just open the activity and make sure it doesn't crash
     */
    @Test
    public void openMainPickerNull() throws InterruptedException {
        //ActivityUtils.openPluginPicker(true, MainActivity.this);
        //ActivityUtils.editChannel("http://google.com");
    }
}
