package com.felkertech.cumulustv.plugins;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.felkertech.cumulustv.fileio.AbstractFileParser;
import com.felkertech.cumulustv.fileio.HttpFileParser;
import com.felkertech.cumulustv.fileio.M3uParser;
import com.felkertech.cumulustv.model.ChannelDatabase;
import com.felkertech.cumulustv.model.ChannelDatabaseFactory;
import com.felkertech.cumulustv.model.JsonChannel;
import com.felkertech.cumulustv.model.JsonListing;

import io.fabric.sdk.android.Fabric;

import com.felkertech.n.cumulustv.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple plugin that alows a user to add a URL pointing to an M3U file which will be continually
 * updated.
 */
public class ListingPlugin extends CumulusTvPlugin {
    private static final String TAG = ListingPlugin.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plugin_json_listing);
        setLabel("");
        setProprietaryEditing(false);
        Fabric.with(this, new Crashlytics());
        Intent i = getIntent();
        if(i.getAction() != null && (i.getAction().equals(Intent.ACTION_SEND) ||
                i.getAction().equals(Intent.ACTION_VIEW))) {
            final Uri uri = getIntent().getData();
            // Give the option to simply link to this Uri.
            importPlaylist(uri);
        } else {
            // The user wants to add / edit an existing item.
            if (areEditing()) {
                try {
                    populate();
                } catch (JSONException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else if (areAdding()) {
                try {
                    populate();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (areReadingAll()) {
                // Show items
                try {
                    showLinks();
                } catch (JSONException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private void importPlaylist(final Uri uri) {
        new MaterialDialog.Builder(this)
                .title(R.string.link_to_m3u)
                .content(getString(R.string.json_link_confirmation, uri))
                .positiveText(R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        save(new JsonListing.Builder()
                            .setUrl(String.valueOf(uri))
                            .build());
                    }
                })
                .show();
    }

    private void populate() throws JSONException {
        if (getJson() != null) {
            JsonListing listing = new JsonListing.Builder(getJson()).build();
            ((EditText) findViewById(R.id.edit_url)).setText(listing.getUrl());
        }

        ((EditText) findViewById(R.id.edit_url)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String url = editable.toString();
                new HttpFileParser(url, new AbstractFileParser.FileLoader() {
                    @Override
                    public void onFileLoaded(InputStream inputStream) {
                        if (inputStream == null) {
                            ((TextView) findViewById(R.id.channel_count)).setText("");
                        } else {
                            try {
                                M3uParser.TvListing listing = M3uParser.parse(inputStream);
                                if (listing != null) {
                                    final List<M3uParser.M3uTvChannel> channels =
                                            listing.channels;
                                    findViewById(R.id.channel_count).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((TextView) findViewById(R.id.channel_count)).setText(
                                                    getString(R.string.x_channels_found, channels.size()));
                                        }
                                    });
                                } else {
                                    findViewById(R.id.channel_count).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((TextView) findViewById(R.id.channel_count)).setText("");
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });

        findViewById(R.id.button_update).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = ((EditText) findViewById(R.id.edit_url)).getText().toString();
                if (!url.isEmpty()) {
                    JsonListing newListing = new JsonListing.Builder()
                            .setUrl(url)
                            .build();
                    save(newListing);
                } else {
                    Toast.makeText(ListingPlugin.this, R.string.msg_url_empty, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLinks() throws JSONException {
        Log.d(TAG, ChannelDatabase.getInstance(this).toString());
        final List<JsonListing> listings = new ArrayList<>();
        final List<String> urlList = new ArrayList<>();
        urlList.add(getString(R.string.close_pretty));
        JSONArray channelData = ChannelDatabase.getInstance(this).getJSONArray();
        for (int i = 0; i < channelData.length(); i++) {
            ChannelDatabaseFactory.parseType(channelData.getJSONObject(i), new ChannelDatabaseFactory.ChannelParser() {
                @Override
                public void ifJsonChannel(JsonChannel entry) {

                }

                @Override
                public void ifJsonListing(JsonListing entry) {
                    listings.add(entry);
                    urlList.add(entry.getUrl());
                }
            });
        }
        String[] urlArray = urlList.toArray(new String[urlList.size()]);

        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Dialog))
                .setTitle(R.string.link_to_m3u)
                .setItems(urlArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position) {
                        if (position == 0) {
                            finish();
                        } else {
                            showEditDialog(listings.get(position - 1));
                        }
                    }
                })
                .show();
    }

    private void showEditDialog(final JsonListing listing) {
        new MaterialDialog.Builder(this)
                .title(R.string.link_to_m3u)
                .content(listing.getUrl())
                .negativeText(R.string.delete)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            ChannelDatabase.getInstance(ListingPlugin.this).delete(listing);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .negativeText(R.string.cancel)
                .show();

    }
}
