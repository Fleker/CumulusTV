package com.felkertech.cumulustv.plugins;

import android.os.Bundle;

/**
 * A simple plugin that alows a user to add a URL pointing to an M3U file which will be continually
 * updated.
 */
public class ListingPlugin extends CumulusTvPlugin {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plugin_json_listing);
    }
}
