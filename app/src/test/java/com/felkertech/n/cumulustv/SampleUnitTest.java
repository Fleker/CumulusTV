package com.felkertech.n.cumulustv;

import android.test.ActivityUnitTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SampleUnitTest extends ActivityUnitTestCase {
    public SampleUnitTest(Class activityClass) {
        super(activityClass);
    }

    @Test
    public void isTrue() {
        assert true;
    }
}
