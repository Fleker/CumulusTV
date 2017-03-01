package com.felkertech.cumulustv.plugins;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.ChannelDatabaseFactory;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.model.JsonListing;
import com.felkertech.cumulustv.model.RecyclerViewItem;
import com.felkertech.cumulustv.ui.RecyclerViewColumnAdapter;
import com.felkertech.n.cumulustv.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nick on 1/25/2017.
 */

public class JsonListingPanelActivity extends Activity {
    private static final String TAG = JsonListingPanelActivity.class.getSimpleName();

    private VerticalGridView mAppLinkMenuList;
    private RecyclerViewItem[] items;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        setContentView(com.felkertech.n.cumulustv.R.layout.activity_quick_settings);
        ((TextView) findViewById(R.id.title)).setText(getString(R.string.link_to_m3u));

        // Sets the size and position of dialog activity.
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        layoutParams.width = getResources().getDimensionPixelSize(R.dimen.side_panel_width);
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(layoutParams);

        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                refreshUi();
            }
        }.sendEmptyMessageDelayed(0, 50);
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

    private JsonListing[] getUrls() throws JSONException {
        final List<JsonListing> listings = new ArrayList<>();
        JSONArray channelData = ChannelDatabase.getInstance(this).getJSONArray();
        for (int i = 0; i < channelData.length(); i++) {
            ChannelDatabaseFactory.parseType(channelData.getJSONObject(i), new ChannelDatabaseFactory.ChannelParser() {
                @Override
                public void ifJsonChannel(JsonChannel entry) {

                }

                @Override
                public void ifJsonListing(JsonListing entry) {
                    listings.add(entry);
                }
            });
        }
        Log.d(TAG, listings.toString());
        return listings.toArray(new JsonListing[listings.size()]);
    }

    private void showEditDialog(final JsonListing listing) {
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.CompatTheme))
                .setTitle(R.string.link_to_m3u)
                .setMessage(listing.getUrl())
                .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Log.d(TAG, "Try deleting " + listing.toString());
                            ChannelDatabase.getInstance(JsonListingPanelActivity.this).delete(listing);
                            new Handler(Looper.getMainLooper()) {
                                @Override
                                public void handleMessage(Message msg) {
                                    super.handleMessage(msg);
                                    refreshUi();
                                }
                            }.sendEmptyMessageDelayed(0, 50);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setPositiveButton(R.string.cancel, null)
                .show();
    }

    private void refreshUi() {
        try {
            final JsonListing[] names = getUrls();
            items = new RecyclerViewItem[names.length + 1];
            items[0] = new RecyclerViewItem(getString(R.string.add_new_link)) {
                @Override
                public void onClick() {
                    Intent i = new Intent(JsonListingPanelActivity.this, ListingPlugin.class);
                    i.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_ADD);
                    startActivity(i);
                }
            };
            if (names.length > 0) {
                for (int i = 1; i < items.length; i++) {
                    final int finalI = i;
                    Log.d(TAG, "Poll " + finalI);
                    items[i] = new RecyclerViewItem(names[finalI - 1].getUrl()) {
                        @Override
                        public void onClick() {
                            showEditDialog(names[finalI - 1]);
                        }
                    };
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mAppLinkMenuList = (VerticalGridView) findViewById(R.id.list);
        mAppLinkMenuList.setAdapter(new AppLinkMenuAdapter(this, items));
    }
}
