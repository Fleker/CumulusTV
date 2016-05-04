/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.felkertech.n.tv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.felkertech.n.ActivityUtils;
import com.felkertech.n.cumulustv.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import io.fabric.sdk.android.Fabric;

/*
 * MainActivity class that loads MainFragment
 */
public class LeanbackActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    /**
     * Called when the activity is first created.
     */
    LeanbackFragment lbf;


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
        Log.d("cumulus:leanback", "Got " + requestCode + " " + resultCode + " from activity");
        ActivityUtils.onActivityResult(this, lbf.gapi, requestCode, resultCode, data);

        Handler h = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                lbf.refreshUI();
            }
        };
        h.sendEmptyMessageDelayed(0, 4000);
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
}
