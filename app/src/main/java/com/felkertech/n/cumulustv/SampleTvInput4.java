package com.felkertech.n.cumulustv;

import android.content.Context;
import android.content.IntentFilter;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.accessibility.CaptioningManager;

/**
 * Created by N on 7/16/2015.
 */
public class SampleTvInput4 extends TvInputService {
    public static String TAG = "cumulus:TvInput";

    @Override
    public void onCreate() {
        super.onCreate();
        /*mHandlerThread = new HandlerThread(getClass()
                .getSimpleName());
        mHandlerThread.start();
        mDbHandler = new Handler(mHandlerThread.getLooper());
        mHandler = new Handler();
        mCaptioningManager = (CaptioningManager)
                getSystemService(Context.CAPTIONING_SERVICE);

        setTheme(android.R.style.Theme_Holo_Light_NoActionBar);

        mSessions = new ArrayList<BaseTvInputSessionImpl>();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager
                .ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager
                .ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);*/
    }

    @Nullable
    @Override
    public Session onCreateSession(String inputId) {
        Log.d(TAG, inputId+" ceate session");
        return null;
    }
}
