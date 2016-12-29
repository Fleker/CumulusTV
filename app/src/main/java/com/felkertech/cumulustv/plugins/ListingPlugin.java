package com.felkertech.cumulustv.plugins;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.felkertech.cumulustv.model.JsonListing;

import io.fabric.sdk.android.Fabric;

/**
 * A simple plugin that alows a user to add a URL pointing to an M3U file which will be continually
 * updated.
 */
public class ListingPlugin extends CumulusTvPlugin {
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
                populate();
            }
        }
    }

    private void importPlaylist(final Uri uri) {
        new MaterialDialog.Builder(this)
                .title(R.string.link_to_m3u)
                .content("Add a reference to " + uri + "? This will continually be updated.")
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

    private void populate() {

    }
}
