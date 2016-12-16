package com.felkertech.cumulustv.test;

import android.os.Build;

import com.felkertech.n.cumulustv.BuildConfig;

import junit.framework.TestCase;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by Nick on 12/15/2016.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.M)
public class M3uParserUnitTest extends TestCase {
}
