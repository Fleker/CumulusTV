package com.felkertech.n.cumulustv.test;

import android.os.Bundle;

import com.felkertech.n.tv.activities.LeanbackActivity;
import com.felkertech.n.tv.fragments.LeanbackFragment;

/**
 * An extension of the Leanback activity and fragment with assertions.
 * @author Nick
 * @version 2016.09.06
 */
public class MockLeanbackActivity extends LeanbackActivity {
    private LeanbackFragment lbf;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leanback);
        lbf = (LeanbackFragment) getFragmentManager().findFragmentById(R.id.main_browse_fragment);
        lbf.mActivity = MockLeanbackActivity.this;
    }
}
