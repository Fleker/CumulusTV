package com.felkertech.cumulustv.plugins;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.felkertech.cumulustv.model.RecyclerViewItem;
import com.felkertech.cumulustv.tv.activities.PlaybackQuickSettingsActivity;
import com.felkertech.cumulustv.utils.ActivityUtils;
import com.felkertech.n.cumulustv.*;
import com.felkertech.n.cumulustv.R;

/**
 * Created by Nick on 1/25/2017.
 */

public class JsonListingPanelActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        setContentView(com.felkertech.n.cumulustv.R.layout.activity_quick_settings);
        PlaybackQuickSettingsActivity.QuickSetting[] quickSettings = new PlaybackQuickSettingsActivity.QuickSetting[3];
    }

    /**
     * Adapter class that provides the app link menu list.
     */
    private class AppLinkMenuAdapter extends RecyclerView.Adapter<PlaybackQuickSettingsActivity.ViewHolder> {
        private PlaybackQuickSettingsActivity.QuickSetting[] mQuickSettings;

        public AppLinkMenuAdapter(PlaybackQuickSettingsActivity.QuickSetting[] quickSettings) {
            mQuickSettings = quickSettings;
        }

        @Override
        public PlaybackQuickSettingsActivity.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = getLayoutInflater().inflate(viewType, mAppLinkMenuList, false);
            return new PlaybackQuickSettingsActivity.ViewHolder(view);
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.item_quick_setting;
        }

        @Override
        public void onBindViewHolder(PlaybackQuickSettingsActivity.ViewHolder viewHolder, final int position) {
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
            return mQuickSettings.length;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    private abstract class QuickSetting extends RecyclerViewItem {
        public QuickSetting(String title) {
            super(title);
        }
    }
}
