package com.felkertech.cumulustv.activities;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RemoteViews;

import com.felkertech.n.cumulustv.R;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.widgets.ChannelShortcut;
import com.felkertech.settingsmanager.SettingsManager;

import org.json.JSONException;

import java.util.List;

/**
 * Saves settings for widgets
 */
public class WidgetSelectionActivity extends AppCompatActivity {
    public static final String EXTRA_APP_WIDGET_ID = "WIDGET_ID";
    public static final String SETTINGS_MANAGER_WIDGET_URL = "WIDGET_URL";
    private int mAppWidgetId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        final ChannelDatabase channelDatabase = ChannelDatabase.getInstance(WidgetSelectionActivity.this);
        String[] channelnames = channelDatabase.getChannelNames();
        try {
            mAppWidgetId = getIntent().getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            displayChannelPicker(channelDatabase.getJsonChannels(), channelnames,
                    getString(R.string.my_channels));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayChannelPicker(final List<JsonChannel> jsonChannels, String[] channelNames,
              String label) {
        new AlertDialog.Builder(WidgetSelectionActivity.this)
                .setTitle(label)
                .setItems(channelNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        JsonChannel jsonChannel = jsonChannels.get(i);
                        SettingsManager settingsManager =
                                new SettingsManager(WidgetSelectionActivity.this);
                        settingsManager.setString(SETTINGS_MANAGER_WIDGET_URL + mAppWidgetId,
                                jsonChannel.getMediaUrl());
                        completeConfiguration();
                    }
                })
                .show();
    }

    private void completeConfiguration() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_channel);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);
        Intent resultValue = new Intent();
        resultValue.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        ChannelShortcut.updateWidgets(this, ChannelShortcut.class);
        finish();
    }

    public static Intent getSetupActivity(int appWidgetId, Context context) {
        Intent intent = new Intent(context, WidgetSelectionActivity.class);
        intent.putExtra(EXTRA_APP_WIDGET_ID, appWidgetId);
        return intent;
    }

    public static JsonChannel getWidgetChannel(Context context, int appWidgetId) {
        ChannelDatabase channelDatabase = ChannelDatabase.getInstance(context);
        String mediaUrl = new SettingsManager(context)
                .getString(SETTINGS_MANAGER_WIDGET_URL + appWidgetId);
        return channelDatabase.findChannelByMediaUrl(mediaUrl);
    }
}
