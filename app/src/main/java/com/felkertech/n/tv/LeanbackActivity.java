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
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.felkertech.n.ActivityUtils;
import com.felkertech.n.boilerplate.Utils.SettingsManager;
import com.felkertech.n.cumulustv.Intro.Intro;
import com.felkertech.n.cumulustv.MainActivity;
import com.felkertech.n.cumulustv.R;

import io.fabric.sdk.android.Fabric;

/*
 * MainActivity class that loads MainFragment
 */
public class LeanbackActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    LeanbackFragment lbf;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leanback);
        lbf = (LeanbackFragment) getFragmentManager().findFragmentById(R.id.main_browse_fragment);
        SettingsManager sm = new SettingsManager(this);
        if(sm.getInt(R.string.sm_last_version) < MainActivity.LAST_GOOD_BUILD) {
            startActivity(new Intent(this, Intro.class));
            finish();
            return;
        }
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
}
