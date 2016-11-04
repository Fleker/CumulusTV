package com.felkertech.n.cumulustv.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import com.bumptech.glide.BitmapTypeRequest;
import com.bumptech.glide.Glide;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.activities.CumulusTvPlayer;
import com.felkertech.n.cumulustv.activities.MainActivity;
import com.felkertech.n.cumulustv.activities.WidgetSelectionActivity;
import com.felkertech.n.cumulustv.model.JsonChannel;
import com.felkertech.n.plugins.MainPicker;

import java.util.concurrent.ExecutionException;

/**
 * A widget implementation which provides a shortcut to a user selected channel on mobile and
 * tablets.
 */
public class ChannelShortcut extends AppWidgetProvider {
    private static final String TAG = ChannelShortcut.class.getSimpleName();
    private static final boolean DEBUG = true;

    private Context context;
    private AppWidgetManager appWidgetManager;
    private int[] appWidgetIds;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        this.context = context;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetIds = appWidgetIds;
        updateWidgets();
    }

        private void updateWidgets() {
        if(appWidgetIds != null) {
            final int N = appWidgetIds.length;
            for (int i = 0; i < N; i++) {
                updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        if (DEBUG) {
            Log.d(TAG, "Added widget to launcher");
        }
        updateWidgets();
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        if (DEBUG) {
            Log.d(TAG, "Removed widget from launcher");
        }
    }

    void updateAppWidget(final Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        final RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget_channel);
        // Get the widget id to get the channel
        final JsonChannel channel = WidgetSelectionActivity.getWidgetChannel(context, appWidgetId);
        if (channel == null) {
            views.setTextViewText(R.id.widget_text, "Reconfigure this widget");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap logo = Glide.with(context)
                            .load(channel.getLogo())
                            .asBitmap()
                            .into(180, 100)
                            .get();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            views.setImageViewBitmap(R.id.widget_image, logo);
                        }
                    });
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        views.setTextViewText(R.id.widget_text, channel.getNumber() + " " + channel.getName());
        Intent i = new Intent(context, CumulusTvPlayer.class);
        i.putExtra(CumulusTvPlayer.KEY_VIDEO_URL, channel.getMediaUrl());

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);
    }
}
