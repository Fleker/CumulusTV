package com.felkertech.n.cumulustv;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;

import com.felkertech.channelsurfer.players.TvInputPlayer;
import com.felkertech.n.boilerplate.Utils.DriveSettingsManager;
import com.felkertech.n.cumulustv.activities.CumulusDreamsSettingsActivity;

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
    private String TAG = "cumulus:Dream";

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Exit dream upon user touch?
        setInteractive(false);

        // Hide system UI?
        setFullscreen(true);

        // Keep screen at full brightness?
        setScreenBright(false);
        Log.d(TAG, "Going to sleep...");
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        Log.d(TAG, "Dream starting");
        DriveSettingsManager sm = new DriveSettingsManager(getApplicationContext());
        final String url = sm.getString(R.string.daydream_url);
        if(!url.isEmpty()) {
            Log.d(TAG, "Play "+url);
            setContentView(R.layout.full_surfaceview);
            SurfaceView sv = (SurfaceView) findViewById(R.id.surface);
            TvInputPlayer exoPlayer;
            exoPlayer = new TvInputPlayer();
            exoPlayer.setSurface(sv.getHolder().getSurface());
            exoPlayer.setVolume(0); //No volume for daydream
            /*exoPlayer.addCallback(new TvInputPlayer.Callback() {
                @Override
                public void onPrepared() {

                }

                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int state) {

                }

                @Override
                public void onPlayWhenReadyCommitted() {

                }

                @Override
                public void onPlayerError(ExoPlaybackException e) {
                    Log.e(TAG, "Callback2");
                    Log.e(TAG, e.getMessage()+"");
                    if(e.getMessage().contains("Extractor")) {
                        Log.d(TAG, "Cannot play the stream, try loading it as a website");
                        WebTvPlayer wv = new WebTvPlayer(getApplication());
                        wv.load(url);
                        setContentView(wv);
                    }
                }

                @Override
                public void onDrawnToSurface(Surface surface) {

                }

                @Override
                public void onText(String text) {

                }
            });*/
            try {
                exoPlayer.prepare(getApplicationContext(), Uri.parse(url), TvInputPlayer.SOURCE_TYPE_HLS);
            } catch(Exception e) {

            }
            exoPlayer.setPlayWhenReady(true);
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
