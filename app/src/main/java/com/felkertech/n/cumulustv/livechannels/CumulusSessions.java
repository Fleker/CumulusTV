package com.felkertech.n.cumulustv.livechannels;

import android.content.Context;
import android.database.Cursor;
import android.media.PlaybackParams;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import com.felkertech.channelsurfer.interfaces.TimeShiftable;
import com.felkertech.channelsurfer.model.Channel;
import com.felkertech.channelsurfer.service.SimpleSessionImpl;
import com.felkertech.channelsurfer.service.TvInputProvider;
import com.felkertech.n.cumulustv.CumulusTvService;

import java.util.Date;

/**
 * Created by Nick on 5/4/2016.
 */
public class CumulusSessions extends SimpleSessionImpl {
    private String TAG = "CSession";
    private TvInputProvider tvInputProvider;
    public CumulusSessions(TvInputProvider tvInputProvider) {
        super(tvInputProvider);
        this.tvInputProvider = tvInputProvider;
    }

    @Override
    public boolean onTune(Uri channelUri) {
        notifyVideoAvailable();
        ((CumulusTvService) tvInputProvider).onPreTune(channelUri);
        new TuningTask().execute(channelUri, this);
        return true;
    }

    /**
     * This AsyncTask is used to do tuning operations in the background in order to reduce the load
     * on the main UI thread. The TuningTask takes in two key parameters in the doInBackground method:
     *   * Uri channelUri: This is the URI corresponding to the channel you're tuning
     *   * SimpleSessionImpl session: `this`, which is used to set variables in the main class
     */
    private class TuningTask extends AsyncTask<Object, Void, Void> {
        Channel channel;
        @Override
        protected Void doInBackground(Object... params) {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);
            Uri channelUri = (Uri) params[0];
            CumulusSessions session = (CumulusSessions) params[1];
            TvContentRating blocked = null; //Reset our channel blocking until we check again
            Log.d(TAG, "Tuning to " + channelUri.toString());
            String[] projection = {TvContract.Channels.COLUMN_DISPLAY_NAME, TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID,
                    TvContract.Channels.COLUMN_SERVICE_ID, TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID, TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                    TvContract.Channels.COLUMN_INPUT_ID, TvContract.Channels.COLUMN_DISPLAY_NUMBER, TvContract.Channels._ID};
            //Now look up this channel in the DB
            try (Cursor cursor = tvInputProvider.getContentResolver().query(channelUri, projection, null, null, null)) {
                if (cursor == null || cursor.getCount() == 0) {
                    return null;
                }
                cursor.moveToNext();
                Log.d(TAG, "Tune to "+cursor.getInt(cursor.getColumnIndex(TvContract.Channels._ID)));
                Log.d(TAG, "And tune 2 "+channelUri);
                channel = new Channel()
                        .setNumber(cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)))
                        .setName(cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)))
                        .setOriginalNetworkId(cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID)))
                        .setTransportStreamId(cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID)))
                        .setServiceId(cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID)))
                        .setInternalProviderData(cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA)))
                        .setChannelId(cursor.getInt(cursor.getColumnIndex(TvContract.Channels._ID)))
                        .setVideoHeight(1080)
                        .setVideoWidth(1920);
                session.currentChannel = channel;

                //Check Content Rating if applicable
                if(tvInputProvider.getProgramRightNow(channel) != null) {
                    TvInputManager mTvInputManager = (TvInputManager) tvInputProvider.getApplicationContext().getSystemService(Context.TV_INPUT_SERVICE);
                    sendToast("Parental controls enabled? " + mTvInputManager.isParentalControlsEnabled());
                    if (mTvInputManager.isParentalControlsEnabled()) {
                        TvContentRating blockedRating = null;
                        for (int i = 0; i < tvInputProvider.getProgramRightNow(channel).getContentRatings().length; i++) {
                            blockedRating = (mTvInputManager.isRatingBlocked(tvInputProvider.getProgramRightNow(channel).getContentRatings()[i]) && blockedRating == null) ? tvInputProvider.getProgramRightNow(channel).getContentRatings()[i] : null;
                        }
                        sendToast("Is channel blocked w/ " + blockedRating + "? Only if not null");
                        blocked = blockedRating;
                        if (blockedRating != null) {
                            notifyContentBlocked(blockedRating);
                        } else {
                            notifyContentAllowed();
                        }
                    }
                }
                Bundle b = new Bundle();
                b.putBoolean("tune", true);
                Message complete = new Message();
                complete.setData(b);
                toaster.sendMessage(complete);
            } catch (Exception e) {
                Log.e(TAG, "Tuning error");
                sendToast("There's an issue w/ tuning: "+e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        Handler toaster = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.getData().getBoolean("tune")) {
                    tvInputProvider.onTune(channel);
                } else {
                    if (tvInputProvider.getApplicationContext().getResources().getBoolean(com.felkertech.channelsurfer.R.bool.channel_surfer_lifecycle_toasts))
                        Toast.makeText(tvInputProvider.getApplicationContext(), msg.getData().getString("msg"), Toast.LENGTH_SHORT).show();
                }
            }
        };

        private void sendToast(String msg) {
            Bundle b = new Bundle();
            b.putString("msg", msg);
            Message m = new Message();
            m.setData(b);
            toaster.sendMessage(m);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}