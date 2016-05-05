package com.felkertech.n.cumulustv.activities;

import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.setup.SimpleTvSetup;
import com.felkertech.channelsurfer.sync.SyncUtils;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.TvContractUtils;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JSONChannel;
import com.felkertech.settingsmanager.SettingsManager;

import org.json.JSONException;

import java.util.List;

import io.fabric.sdk.android.Fabric;

/**
 * Created by N on 7/12/2015.
 */
public class CumulusChannelsSetup extends SimpleTvSetup {
    private String TAG = "cumulus:CumulusChannelsSetup";
    private String ABCNews = "http://abclive.abcnews.com/i/abc_live4@136330/index_1200_av-b.m3u8";
    public static String COLUMN_CHANNEL_URL = "CHANNEL_URL";
    private static int SETUP_DURATION = 10*1000;
    private static int SETUP_UI_LAST = 5*1000; //DURATION - UI_LAST seconds is the last time UI list runs

    @Override
    public void setupTvInputProvider() {
        setContentView(R.layout.activity_setup);
        Log.d(TAG, "Created me");

        Fabric.with(this, new Crashlytics());
        String info = "";
        if(getIntent() != null) {
            info = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
            Log.d(TAG, info);
        }


        ChannelDatabase cd = new ChannelDatabase(this);
        List<Channel> list = null;
        try {
            list = cd.getChannels();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        TvContractUtils.updateChannels(this, info, list);
        SyncUtils.setUpPeriodicSync(this, info);
        SyncUtils.requestSync(this, info);
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
                finishSetup();
                killer.sendEmptyMessageDelayed(0, 10);
            }
        };
        h.sendEmptyMessageDelayed(0, SETUP_DURATION);

        //This is a neat little UI thing for people who have channels
        final int[] channelIndex = new int[]{0};
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

        String[] projection = {TvContract.Channels.COLUMN_DISPLAY_NUMBER, TvContract.Channels.COLUMN_DISPLAY_NAME, TvContract.Channels._ID};
        //Now look up this channel in the DB
        try (Cursor cursor = getContentResolver().query(TvContract.buildChannelsUriForInput(null), projection, null, null, null)) {
            if (cursor == null || cursor.getCount() == 0) {
                return;
            }
            while(cursor.moveToNext()) {
                //cursor.moveToNext();
                Log.d(TAG, "Tune to " + cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)));
                JSONChannel jsonChannel = new ChannelDatabase(this).findChannel(cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)));
                Log.d(TAG, jsonChannel.toString());
                Log.d(TAG, cursor.getInt(cursor.getColumnIndex(TvContract.Channels._ID)) + "");
                new SettingsManager(this).setString("URI" + cursor.getInt(cursor.getColumnIndex(TvContract.Channels._ID)), jsonChannel.getNumber());
                Log.d(TAG, new SettingsManager(this).getString("URI" + cursor.getInt(cursor.getColumnIndex(TvContract.Channels._ID))));
            }
        } catch (Exception e) {
            Log.e(TAG, "Tuning error");
            e.printStackTrace();
        }
    }

    public void finishSetup() {
        super.setupTvInputProvider();
    }
}
