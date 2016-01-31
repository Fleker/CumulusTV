package com.felkertech.n.cumulustv;

import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.example.android.sampletvinput.data.Program;
import com.example.android.sampletvinput.player.TvInputPlayer;
import com.example.android.sampletvinput.syncadapter.SyncAdapter;
import com.example.android.sampletvinput.syncadapter.SyncUtils;

import java.util.List;

import io.fabric.sdk.android.Fabric;

/**
 * Created by N on 7/12/2015.
 */
public class SampleSetup extends AppCompatActivity {
    private String TAG = "cumulus:SampleSetup";
    private String ABCNews = "http://abclive.abcnews.com/i/abc_live4@136330/index_1200_av-b.m3u8";
    public static String COLUMN_CHANNEL_URL = "CHANNEL_URL";
    private static int SETUP_DURATION = 30*1000;
    private static int SETUP_UI_LAST = 5*1000; //DURATION - UI_LAST seconds is the last time UI list runs
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Log.d(TAG, "Created me");

        Fabric.with(this, new Crashlytics());
        String info = "";
        if(getIntent() != null) {
             info = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
            Log.d(TAG, info);
        }


        List<TvManager.ChannelInfo> list = SyncAdapter.getChannels(this);

        TvContractUtils.updateChannels(this, info, list);
        SyncUtils.setUpPeriodicSync(this, info);
        SyncUtils.requestSync(info);
        boolean mSyncRequested = true;
        Log.d(TAG, "Everything happened");

        final Handler killer = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                finish();
            }
        };

        Handler h = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Toast.makeText(getApplicationContext(), "Setup complete. Make sure you enable these channels in the channel list.", Toast.LENGTH_SHORT).show();
                killer.sendEmptyMessageDelayed(0, 10);
            }
        };
        h.sendEmptyMessageDelayed(0, SETUP_DURATION);

        //This is a neat little UI thing for people who have channels
        final int[] channelIndex = new int[]{0};
        ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
        final String[] channels = cd.getChannelNames();
        Log.d(TAG, "Run for "+channels.length+" times");
        if(channels.length <= 0) {
            Log.d(TAG, "What can you do if you have no channels");
            return;
        }
        Handler i = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                TextView channelsList = (TextView) findViewById(R.id.channel_list);
                if(channelIndex[0] < channels.length)
                    channelsList.setText(channelsList.getText()+"\n"+channels[channelIndex[0]]);
                channelIndex[0]++;
                if(channelIndex[0] <= channels.length)
                    sendEmptyMessageDelayed(0, (SETUP_DURATION-SETUP_UI_LAST)/channels.length);
            }
        };
        i.sendEmptyMessageDelayed(0, (SETUP_DURATION-SETUP_UI_LAST)/channels.length);
    }
}
