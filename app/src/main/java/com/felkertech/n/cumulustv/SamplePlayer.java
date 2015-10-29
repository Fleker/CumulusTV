package com.felkertech.n.cumulustv;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.example.android.sampletvinput.player.TvInputPlayer;

import java.net.URL;

/**
 * Created by N on 7/12/2015.
 */
public class SamplePlayer extends AppCompatActivity {
    private String urlStream;
    private VideoView myVideoView;
    private URL url;
    private String TAG = "cumulus:SamplePlayer";
    public static final String KEY_VIDEO_URL = "VIDEO_URL";

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
            String url = parameters.getStringExtra(KEY_VIDEO_URL);
            if(!url.isEmpty()) {
                setContentView(R.layout.full_surfaceview);
                SurfaceView sv = (SurfaceView) findViewById(R.id.surface);
                TvInputPlayer exoPlayer;
                exoPlayer = new TvInputPlayer();
                exoPlayer.setSurface(sv.getHolder().getSurface());
                exoPlayer.setVolume(1);
                exoPlayer.prepare(getApplicationContext(), Uri.parse(url), TvInputPlayer.SOURCE_TYPE_HLS);
                exoPlayer.setPlayWhenReady(true);
            }
        }
    }
}
