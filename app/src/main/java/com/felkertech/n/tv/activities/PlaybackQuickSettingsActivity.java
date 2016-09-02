package com.felkertech.n.tv.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.felkertech.n.ActivityUtils;
import com.felkertech.n.cumulustv.R;
import com.felkertech.n.cumulustv.exceptions.PlaybackIssueException;
import com.felkertech.n.cumulustv.model.JsonChannel;

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

        QuickSetting[] quickSettings = new QuickSetting[3];
        try {
            final JsonChannel jsonChannel = new JsonChannel.Builder(getIntent()
                    .getStringExtra(EXTRA_JSON_CHANNEL)).build();

            // Open this channel in the editor
            quickSettings[0] = new QuickSetting(
                    getString(R.string.edit_channel_name, jsonChannel.getName())) {
                @Override
                public void onClick() {
                    ActivityUtils.editChannel(PlaybackQuickSettingsActivity.this,
                            jsonChannel.getNumber());
                }
            };

            // Open CumulusTV
            quickSettings[1] = new QuickSetting(getString(R.string.open_cumulus_tv)) {
                @Override
                public void onClick() {
                    startActivity(new Intent(PlaybackQuickSettingsActivity.this,
                            ActivityUtils.getMainActivity(PlaybackQuickSettingsActivity.this)));
                }
            };

            quickSettings[2] = new QuickSetting(getString(R.string.report_playback_issue)) {
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

        setContentView(R.layout.activity_quick_settings);

        mAppLinkMenuList = (VerticalGridView) findViewById(R.id.list);
        mAppLinkMenuList.setAdapter(new AppLinkMenuAdapter(quickSettings));
    }

    public static Intent getIntent(Context context, String json) {
        Intent intent = new Intent(context, PlaybackQuickSettingsActivity.class);
        intent.putExtra(EXTRA_JSON_CHANNEL, json);
        return intent;
    }

    /**
     * Adapter class that provides the app link menu list.
     */
    public class AppLinkMenuAdapter extends RecyclerView.Adapter<ViewHolder> {
        private static final int ITEM_COUNT = 2;
        private QuickSetting[] mQuickSettings;

        public AppLinkMenuAdapter(QuickSetting[] quickSettings) {
            mQuickSettings = quickSettings;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = getLayoutInflater().inflate(viewType, mAppLinkMenuList, false);
            return new ViewHolder(view);
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.item_quick_setting;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            TextView view = (TextView) viewHolder.itemView;
            view.setText(mQuickSettings[position].title);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mQuickSettings[position].onClick();
                }
            });
        }

        @Override
        public int getItemCount() {
            return ITEM_COUNT;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    private abstract class QuickSetting {
        public final String title;

        public QuickSetting(String title) {
            this.title = title;
        }
        public abstract void onClick();
    }
}