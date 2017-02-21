package com.felkertech.cumulustv.ui;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.felkertech.cumulustv.model.RecyclerViewItem;
import com.felkertech.cumulustv.tv.activities.PlaybackQuickSettingsActivity;
import com.felkertech.n.cumulustv.R;

/**
 * Created by Nick on 1/25/2017.
 */

public abstract class RecyclerViewColumnAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private RecyclerViewItem[] mItems;
    private Activity mActivity;

    public RecyclerViewColumnAdapter(Activity context, RecyclerViewItem[] items) {
        mActivity = context;
        mItems = items;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = mActivity.getLayoutInflater().inflate(viewType, null, false);
        return createNewViewHolder(view);
    }

    /**
     * Just return a new {@link RecyclerView.ViewHolder} with your custom ViewHolder class.
     * @param view
     * @return
     */
    public abstract RecyclerView.ViewHolder createNewViewHolder(View view);

    @Override
    public int getItemViewType(int position) {
        return R.layout.item_quick_setting;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        TextView view = (TextView) viewHolder.itemView;
        view.setText(mItems[position].title);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItems[position].onClick();
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mItems == null) {
            return 0;
        }
        return mItems.length;
    }
}
