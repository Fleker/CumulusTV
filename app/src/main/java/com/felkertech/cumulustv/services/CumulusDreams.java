package com.felkertech.cumulustv.services;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;

import com.felkertech.cumulustv.player.CumulusTvPlayer;
import com.felkertech.n.cumulustv.R;
import com.felkertech.cumulustv.utils.DriveSettingsManager;
import com.felkertech.cumulustv.activities.CumulusDreamsSettingsActivity;

/**
 * This class is a sample implementation of a DreamService. When activated, a
 * TextView will repeatedly, move from the left to the right of screen, at a
 * random y-value.
 * The generated {@link CumulusDreamsSettingsActivity} allows
 * the user to change the text which is displayed.
 * <p/>
 * Daydreams are only available on devices running API v17+.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class CumulusDreams extends DreamService {
    private static final String TAG = CumulusDreams.class.getSimpleName();
    private static final boolean DEBUG = false;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Exit dream upon user touch?
        setInteractive(false);

        // Hide system UI?
        setFullscreen(true);

        // Keep screen at full brightness?
        setScreenBright(false);
        if (DEBUG) {
            Log.d(TAG, "Going to sleep...");
        }
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (DEBUG) {
            Log.d(TAG, "Dream starting");
        }
        DriveSettingsManager sm = new DriveSettingsManager(getApplicationContext());
        final String url = sm.getString(R.string.daydream_url);
        if(!url.isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "Play " + url);
            }
            setContentView(R.layout.full_surfaceview);
            SurfaceView sv = (SurfaceView) findViewById(R.id.surface);
//            CumulusVideoPlayback cumulusTvPlayer = new CumulusVideoPlayback();
//            exoPlayer.setSurface(sv.getHolder().getSurface());
//            exoPlayer.setVolume(0); //No volume for daydream

//            try {
//                exoPlayer.prepare(getApplicationContext(), Uri.parse(url), TvInputPlayer.SOURCE_TYPE_HLS);
//            } catch(Exception ignored) {
//            }
//            exoPlayer.setPlayWhenReady(true);
        } else {
            TextView EMPTY_URL = new TextView(getApplicationContext());
            EMPTY_URL.setText("THIS URL IS EMPTY");
            setContentView(EMPTY_URL);
        }
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
