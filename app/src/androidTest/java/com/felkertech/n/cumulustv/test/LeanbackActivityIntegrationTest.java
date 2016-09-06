package com.felkertech.n.cumulustv.test;

import android.test.ActivityInstrumentationTestCase2;

import com.felkertech.n.tv.activities.LeanbackActivity;
import com.felkertech.n.tv.fragments.LeanbackFragment;

import org.junit.runner.RunWith;

/**
 * Tests common activities in the {@link LeanbackActivity} and {@link LeanbackFragment}
 * @author Nick
 * @version 2016.09.06
 */
@RunWith(AndroidJUnit4.class)
public class LeanbackActivityIntegrationTest
        extends ActivityInstrumentationTestCase2<MockLeanbackActivity> {
    public LeanbackActivityIntegrationTest() {
        super(MockLeanbackActivity.class);
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        getActivity();
    }
}
