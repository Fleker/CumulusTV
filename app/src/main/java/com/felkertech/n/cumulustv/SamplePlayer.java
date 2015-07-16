package com.felkertech.n.cumulustv;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.widget.MediaController;
import android.widget.VideoView;

import java.net.URL;

/**
 * Created by N on 7/12/2015.
 */
public class SamplePlayer extends AppCompatActivity {
    private String urlStream;
    private VideoView myVideoView;
    private URL url;
    private String TAG = "cumulus:SamplePlayer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Doing it the web-based way */
//        setContentView(R.layout.fullweb);
//        WebView wv = (WebView) findViewById(R.id.browser);
//        wv.loadUrl("http://abclive.abcnews.com/i/abc_live4@136330/index_1200_av-b.m3u8"); //ABC News
        /*wv.loadData("<video id=example-video width=600 height=300 class='video-js vjs-default-skin' controls>" +
                "<source src='http://abclive.abcnews.com/i/abc_live4@136330/index_1200_av-b.m3u8'" +
                "     type='application/x-mpegURL'>" +
                "</video>" +
                "<script src='http://videojs.github.io/videojs-contrib-hls/node_modules/video.js/dist/video-js/video.dev.js'></script>" +
                "<script src='http://videojs.github.io/videojs-contrib-hls/node_modules/videojs-contrib-media-sources/src/videojs-media-sources.js'></script>" +
                "<script src='https://github.com/videojs/videojs-contrib-hls/releases/download/v0.17.1/videojs.hls.min.js'></script>" +
                "<script>" +
                "var player; console.log(11);" +
                "videojs('example-video').ready(function(){" +
                "  player = this;" +
                "  // EXAMPLE: Start playing the video." +
                "  console.log(12); myPlayer.play();" +
                "});" +
                "</script>", "text/html", null);*/

        /* Doing it the native way */
        setContentView(R.layout.fullvideo);//***************
        myVideoView = (VideoView)this.findViewById(R.id.myVideoView);
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
    }
    //TODO Fix launcher, direct to setp system
}
