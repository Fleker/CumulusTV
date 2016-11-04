package com.felkertech.n.cumulustv.activities;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.model.ChannelDatabase;
import com.felkertech.n.cumulustv.model.JsonChannel;
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
        new MaterialDialog.Builder(WidgetSelectionActivity.this)
                .title(label)
                .items(channelNames)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, final int i, CharSequence charSequence) {
                        JsonChannel jsonChannel = jsonChannels.get(i);
                        SettingsManager settingsManager =
                                new SettingsManager(WidgetSelectionActivity.this);
                        settingsManager.setString(SETTINGS_MANAGER_WIDGET_URL + mAppWidgetId,
                                jsonChannel.getMediaUrl());
                    }
                })
                .show();
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
