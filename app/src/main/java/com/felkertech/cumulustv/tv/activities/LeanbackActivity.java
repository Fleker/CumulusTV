package com.felkertech.cumulustv.tv.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.felkertech.cumulustv.fileio.CloudStorageProvider;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.n.cumulustv.R;
import com.felkertech.cumulustv.tv.fragments.LeanbackFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import io.fabric.sdk.android.Fabric;

/*
 * MainActivity class that loads MainFragment
 */
public class LeanbackActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = LeanbackActivity.class.getSimpleName();

    public static final int RESULT_CODE_REFRESH_UI = 10;
    @VisibleForTesting
    public static LeanbackFragment lbf;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leanback);
        lbf = (LeanbackFragment) getFragmentManager().findFragmentById(R.id.main_browse_fragment);
        lbf.mActivity = LeanbackActivity.this;
        ActivityUtils.openIntroIfNeeded(this);
        Fabric.with(this, new Crashlytics());
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(TAG, "Got " + requestCode + " " + resultCode + " from activity");
        ActivityUtils.onActivityResult(this, CloudStorageProvider.getInstance().getClient(),
                requestCode, resultCode, data);
        if (requestCode == RESULT_CODE_REFRESH_UI) {
            lbf.refreshUI();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        lbf.refreshUI();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        lbf.onConnected(bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        lbf.onConnectionSuspended(i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        lbf.onConnectionFailed(connectionResult);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lbf = null;
    }
}
