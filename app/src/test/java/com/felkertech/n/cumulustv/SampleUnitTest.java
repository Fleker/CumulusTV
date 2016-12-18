package com.felkertech.n.cumulustv;

import android.test.ActivityUnitTestCase;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SampleUnitTest extends TestCase {
    public SampleUnitTest(Class activityClass) {
    }

    @Test
    public void isTrue() {
        assert true;
    }
}
