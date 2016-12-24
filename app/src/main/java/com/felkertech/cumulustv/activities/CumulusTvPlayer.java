package com.felkertech.cumulustv.activities;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.n.cumulustv.R;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Nick on 7/12/2015.
 */
@Deprecated
public class CumulusTvPlayer extends AppCompatActivity {
    private String urlStream;
    private VideoView myVideoView;
    private String TAG = "cumulus:CumulusTvPlayer";
    public static final String KEY_VIDEO_URL = "VIDEO_URL";
    private com.felkertech.cumulustv.player.CumulusTvPlayer mTvPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Doing it the native way */
        Intent parameters = getIntent();
        if(parameters == null) {
            setContentView(R.layout.fullvideo);//***************
            myVideoView = (VideoView) this.findViewById(R.id.myVideoView);
            MediaController mc = new MediaController(this);
            myVideoView.setMediaController(mc);
            String ABCNews = "http://abclive.abcnews.com/i/abc_live4@136330/index_1200_av-b.m3u8";
            String Brazil = "http://stream331.overseebrasil.com.br/live_previd_155/_definst_/live_previd_155/playlist.m3u8";
            String NASA = "http://www.nasa.gov/multimedia/nasatv/NTV-Public-IPS.m3u8";
            urlStream = ABCNews;
            Log.d(TAG, "About to open " + urlStream.toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "On UI Thread");
                    myVideoView.setVideoURI(Uri.parse(urlStream));
                    Log.d(TAG, "Now play");
                    myVideoView.start();
                }
            });
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getSupportActionBar().hide();

            updateLauncherShortcut();

            urlStream = parameters.getStringExtra(KEY_VIDEO_URL);
            if(!urlStream.isEmpty()) {
                setContentView(R.layout.full_surfaceview);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTvPlayer.stop();
        mTvPlayer.release();
    }

    private void updateLauncherShortcut() {
        // We will have one dynamic shortcut - to whichever stream was played last
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    JsonChannel jsonChannel = ChannelDatabase.getInstance(getApplicationContext())
                            .findChannelByMediaUrl(urlStream);
                    Log.d(TAG, "Adding dynamic shortcut to " + jsonChannel.getName());
                    String logo = ChannelDatabase.getNonNullChannelLogo(jsonChannel);
                    try {
                        Bitmap logoBitmap = Picasso.with(getApplicationContext())
                                .load(logo)
                                .get();
                        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                        shortcutManager.removeAllDynamicShortcuts();
                        Intent playVideo = new Intent(getApplicationContext(), CumulusTvPlayer.class);
                        playVideo.setAction("play");
                        playVideo.putExtra(KEY_VIDEO_URL, urlStream);
                        ShortcutInfo shortcut = new ShortcutInfo.Builder(CumulusTvPlayer.this, "id1")
                                .setShortLabel(jsonChannel.getName())
                                .setLongLabel(jsonChannel.getNumber() + " - " + jsonChannel.getName())
                                .setIcon(Icon.createWithBitmap(logoBitmap))
                                .setIntent(playVideo)
                                .build();
                        shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
