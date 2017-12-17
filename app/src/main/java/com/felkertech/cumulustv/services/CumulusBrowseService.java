package com.felkertech.cumulustv.services;

import android.app.Notification;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.auto.CustomMediaSession;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.tv.CumulusTvTifService;
import com.felkertech.n.cumulustv.R;
import com.google.android.media.tv.companionlibrary.model.Channel;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick on 3/1/2017.
 */

public class CumulusBrowseService extends MediaBrowserServiceCompat {
    private static final String TAG = CumulusBrowseService.class.getSimpleName();

    private static final String MEDIA_ROOT_ID = "root";
    private MediaSessionCompat mSession;

    @Override
    public void onCreate() {
        super.onCreate();
        mSession = new MediaSessionCompat(this, "session tag");
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(new CustomMediaSession(getApplicationContext(), new CustomMediaSession.Callback() {
            @Override
            public void onPlaybackStarted(JsonChannel channel) {
                showNotification(channel);
            }

            @Override
            public void onPlaybackEnded() {
                stopForeground(true);
                removeNotification();
            }
        }));
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSession.release();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String s, int i, @Nullable Bundle bundle) {
        // Verify that the specified package is allowed to access your
        // content! You'll need to write your own logic to do this.
//        if (!isValid(clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return null.
            // No further calls will be made to other media browsing methods.

//            return null;
//        }

        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        ChannelDatabase channelDatabase = ChannelDatabase.getInstance(getApplicationContext());
        if (parentId.equals(MEDIA_ROOT_ID)) {
            try {
                for (Channel channel : channelDatabase.getChannels()) {
                    MediaDescriptionCompat descriptionCompat = new MediaDescriptionCompat.Builder()
                            .setMediaId(channel.getInternalProviderData().getVideoUrl())
                            .setTitle(channel.getDisplayName())
                            .setIconUri(Uri.parse(channel.getChannelLogo()))
                            .setSubtitle(getString(R.string.channel_no_xxx, channel.getDisplayNumber()))
                            .setDescription(channel.getDescription())
                            .setMediaUri(Uri.parse(channel.getInternalProviderData().getVideoUrl()))
                            .build();
                    mediaItems.add(new MediaBrowserCompat.MediaItem(descriptionCompat,
                            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
                }
                result.sendResult(mediaItems);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void showNotification(JsonChannel channel) {
        Notification notification = createNotification(channel);
        Log.d(TAG, "Starting foreground service");
        startForeground(1, notification);
    }

    private void removeNotification() {
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
        mNotificationManager.cancelAll();
    }

    private Notification createNotification(JsonChannel channel) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(channel.getName())
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setUsesChronometer(true);
        notificationBuilder.setStyle(new android.support.v7.app.NotificationCompat.MediaStyle()
                .setMediaSession(mSession.getSessionToken()));
        return notificationBuilder.build();
    }
}
