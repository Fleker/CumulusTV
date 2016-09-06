package com.felkertech.n.cumulustv.test;

import android.os.Bundle;

import com.felkertech.n.tv.fragments.LeanbackFragment;

import junit.framework.Assert;

/**
 * An extension of LeanbackFragment with assertions
 * @author Nick
 * @version 2016.09.06
 */
public class MockLeanbackFragment extends LeanbackFragment {
    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Assert.assertNotNull("Activity is null", getActivity());
    }

    @Override
    public void refreshUI() {
        super.refreshUI();
        Assert.assertNotNull("Activity is null", getActivity());
    }
}