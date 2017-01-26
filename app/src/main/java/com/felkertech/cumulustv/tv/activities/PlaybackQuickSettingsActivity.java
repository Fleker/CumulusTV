package com.felkertech.cumulustv.tv.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.icu.util.RangeValueIterator;
import android.os.Bundle;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.felkertech.cumulustv.model.RecyclerViewItem;
import com.felkertech.cumulustv.ui.RecyclerViewColumnAdapter;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.n.cumulustv.R;
import com.felkertech.cumulustv.exceptions.PlaybackIssueException;
import com.felkertech.cumulustv.model.JsonChannel;

import org.json.JSONException;

/**
 * Activity that shows a simple side panel UI.
 *
 * @author Nick
 * @version 2016-09-02
 */
public class PlaybackQuickSettingsActivity extends Activity {
    public static final String EXTRA_JSON_CHANNEL = "JSON_CHANNEL";

    private VerticalGridView mAppLinkMenuList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        setContentView(R.layout.activity_quick_settings);

        RecyclerViewItem[] quickSettings = new RecyclerViewItem[3];
        try {
            final JsonChannel jsonChannel = new JsonChannel.Builder(getIntent()
                    .getStringExtra(EXTRA_JSON_CHANNEL)).build();

            // Set the title
            ((TextView) findViewById(R.id.title)).setText(jsonChannel.getName());

            // Open this channel in the editor
            quickSettings[0] = new RecyclerViewItem(
                    getString(R.string.edit_channel_name, jsonChannel.getName())) {
                @Override
                public void onClick() {
                    ActivityUtils.editChannel(PlaybackQuickSettingsActivity.this,
                            jsonChannel.getMediaUrl());
                }
            };

            // Open CumulusTV
            quickSettings[1] = new RecyclerViewItem(getString(R.string.open_cumulus_tv)) {
                @Override
                public void onClick() {
                    startActivity(new Intent(PlaybackQuickSettingsActivity.this,
                            ActivityUtils.getMainActivity(PlaybackQuickSettingsActivity.this)));
                }
            };

            // Sends a crash report
            quickSettings[2] = new RecyclerViewItem(getString(R.string.report_playback_issue)) {
                @Override
                public void onClick() {
                    throw new PlaybackIssueException("Issue found with playback: " +
                            jsonChannel.toString());
                }
            };
        } catch (JSONException e) {
            Toast.makeText(this, R.string.toast_error_sorry, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Sets the size and position of dialog activity.
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        layoutParams.width = getResources().getDimensionPixelSize(R.dimen.side_panel_width);
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(layoutParams);

        mAppLinkMenuList = (VerticalGridView) findViewById(R.id.list);
        mAppLinkMenuList.setAdapter(new AppLinkMenuAdapter(this, quickSettings));
    }

    public static Intent getIntent(Context context, JsonChannel jsonChannel) {
        Intent intent = new Intent(context, PlaybackQuickSettingsActivity.class);
        intent.putExtra(EXTRA_JSON_CHANNEL, jsonChannel.toString());
        return intent;
    }

    /**
     * Adapter class that provides the app link menu list.
     */
    private class AppLinkMenuAdapter extends RecyclerViewColumnAdapter {
        public AppLinkMenuAdapter(Activity activities, RecyclerViewItem[] quickSettings) {
            super(activities, quickSettings);
        }

        @Override
        public RecyclerView.ViewHolder createNewViewHolder(View view) {
            return new ViewHolder(view);
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}