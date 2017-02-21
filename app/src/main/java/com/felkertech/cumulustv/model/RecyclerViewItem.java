package com.felkertech.cumulustv.model;

/**
 * An abstract class which exists to display an option inside of a RecyclerView.
 *
 * @see com.felkertech.cumulustv.tv.activities.PlaybackQuickSettingsActivity.QuickSetting
 */
public abstract class RecyclerViewItem {
    public final String title;

    public RecyclerViewItem(String title) {
        this.title = title;
    }
    public abstract void onClick();
}
