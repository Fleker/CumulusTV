package com.felkertech.n.cumulustv;

import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.android.sampletvinput.player.TvInputPlayer;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by N on 7/12/2015.
 */
public class SampleSetup extends AppCompatActivity {
    private String TAG = "cumulus:SampleSetup";
    private String ABCNews = "http://abclive.abcnews.com/i/abc_live4@136330/index_1200_av-b.m3u8";
    public static String COLUMN_CHANNEL_URL = "CHANNEL_URL";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "Setup complete", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Created me");
        String info = "";
        if(getIntent() != null) {
             info = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
            Log.d(TAG, info);
        }
        //TODO In the future have a list of stored channels. Now just hack it in
        ContentValues values = new ContentValues();

        List<TvManager.ProgramInfo> infoList = new ArrayList<TvManager.ProgramInfo>();
        TvContentRating rating = TvContentRating.createRating(
                "com.android.tv",
                "US_TV",
                "US_TV_PG",
                "US_TV_D", "US_TV_L");
        infoList.add(new TvManager.ProgramInfo("The News", "https://yt3.ggpht.com/-F2fw6o3bXkE/AAAAAAAAAAI/AAAAAAAAAAA/DMJGHPkK9As/s88-c-k-no/photo.jpg",
                "Dr. Richard Besser", 60*60, new TvContentRating[] {rating}, new String[] {TvContract.Programs.Genres.NEWS}, ABCNews, TvInputPlayer.SOURCE_TYPE_HTTP_PROGRESSIVE, 0));
        TvManager.ChannelInfo channel = new TvManager.ChannelInfo("6", "Custom Channel", "https://yt3.ggpht.com/-F2fw6o3bXkE/AAAAAAAAAAI/AAAAAAAAAAA/DMJGHPkK9As/s88-c-k-no/photo.jpg",
                1337,0, 1, 1920, 1080, infoList);

        Log.d(TAG, channel.originalNetworkId+" "+channel.transportStreamId+" "+channel.serviceId);
        values.put(TvContract.Channels.COLUMN_INPUT_ID, info);

        values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, channel.number);
        values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, channel.name);
//        values.put(TvContract.Channels.COLUMN_INPUT_ID, "CumulusTV");
//        values.put(TvContract.Channels.COLUMN_INPUT_ID, TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", "SampleSetup")));
        values.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, 0);
        values.put(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, 0);
        values.put(TvContract.Channels.COLUMN_SERVICE_ID, 1);
        values.put(TvContract.Channels.COLUMN_SERVICE_TYPE, TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO);
        values.put(TvContract.Channels.COLUMN_VIDEO_FORMAT, TvContract.Channels.VIDEO_FORMAT_1080P);
        values.put(TvContract.Channels.COLUMN_TYPE, TvContract.Channels.TYPE_OTHER);

        Uri uri = getContentResolver().insert(TvContract.Channels.CONTENT_URI, values);
        Log.d(TAG, "finish");
//        finish();
    }
}
