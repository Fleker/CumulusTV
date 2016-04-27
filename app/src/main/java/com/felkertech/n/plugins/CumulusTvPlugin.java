package com.felkertech.n.plugins;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.felkertech.n.cumulustv.model.JSONChannel;
import com.felkertech.n.cumulustv.R;

/**
 * Created by guest1 on 8/7/2015.
 */
public class CumulusTvPlugin extends AppCompatActivity {
    public static String INTENT_EDIT = "Edit";
    public static String INTENT_ADD = "Add";
    public static String INTENT_EXTRA_ACTION = "action";
    public static String INTENT_EXTRA_NUMBER = "Number";
    public static String INTENT_EXTRA_NAME = "Name";
    public static String INTENT_EXTRA_ICON = "Icon";
    public static String INTENT_EXTRA_URL = "Url";
    public static String INTENT_EXTRA_SPLASH = "Splash";
    public static String INTENT_EXTRA_GENRES = "Genres";
    public static String TAGx = "cumulus:Plugin";
    public static String INTENT_EXTRA_READ_ALL = "Readall";
    public static String INTENT_EXTRA_ALL_CHANNELS = "Allchannels";

    private Intent telegram;
    private boolean isEdit;
    private String label;
    private boolean proprietary = true;

    /**
     * Starts the activity. You can override this to inflate a layout and setup anything else
     * you need
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        telegram = getIntent();
        if(telegram.hasExtra(INTENT_EXTRA_ACTION))
            isEdit = telegram.getStringExtra(INTENT_EXTRA_ACTION).equals(INTENT_EDIT);
        else
            isEdit = false;
        Log.d(TAGx, "Initialized");

        setContentView(R.layout.loading);
    }

    /**
     * This label will be used to identify your plugin
     * @param l A simple user-facing String that tells the user the name of your plugin
     */
    public void setLabel(String l) {
        label = l;
    }

    /**
     * Determines whether you're adding or editing a channel
     * @return
     */
    public boolean areEditing() {
        return isEdit;
    }

    /**
     * Determines whether you are reading all user channels or have just specified a single one
     * @return
     */
    public boolean areReadingAll() {
        return telegram.getStringExtra(INTENT_EXTRA_ACTION).equals(INTENT_EXTRA_READ_ALL);
    }

    /**
     * Writes the data to the local file and then forces a data sync. Then the app closes.
     * @param jsonChannel The jsonchannel you wanted to create or update
     */
    public void saveChannel(JSONChannel jsonChannel) {
        String jsonString = jsonChannel.toString();
//        Intent i = new Intent("com.felkertech.cumulustv.RECEIVER");
        Intent i = new Intent();
        /*
        com.felkertech.n.cumulustv com.felkertech.n.plugins.MainPicker
         */
        i.setClassName("com.felkertech.n.cumulustv",
                "com.felkertech.n.plugins.DataReceiver");
        i.setAction("com.felkertech.cumulustv.RECEIVER");
        i.putExtra(DataReceiver.INTENT_EXTRA_JSON, jsonString);
        if(proprietary)
            i.putExtra(DataReceiver.INTENT_EXTRA_SOURCE, getApplicationInfo().packageName+","+getApplicationInfo().name);
        else
            i.putExtra(DataReceiver.INTENT_EXTRA_SOURCE, "");
        i.putExtra(DataReceiver.INTENT_EXTRA_ACTION, DataReceiver.INTENT_EXTRA_ACTION_WRITE);
        Log.d("cumulus:plugin", "Saving changes");
        sendBroadcast(i);
        finish();
    }
    public void saveChannel(JSONChannel newChannel, JSONChannel original) {
        String jsonString = newChannel.toString();
        Intent i = new Intent();
        String ogString = original.toString();
        i.setClassName("com.felkertech.n.cumulustv",
                "com.felkertech.n.plugins.DataReceiver");
        i.setAction("com.felkertech.cumulustv.RECEIVER");
        i.putExtra(DataReceiver.INTENT_EXTRA_JSON, jsonString);
        if(getChannel() != null)
            i.putExtra(DataReceiver.INTENT_EXTRA_ORIGINAL_JSON, ogString);
        if(proprietary)
            i.putExtra(DataReceiver.INTENT_EXTRA_SOURCE, getApplicationInfo().packageName+","+getApplicationInfo().name);
        else
            i.putExtra(DataReceiver.INTENT_EXTRA_SOURCE, "");
        i.putExtra(DataReceiver.INTENT_EXTRA_ACTION, DataReceiver.INTENT_EXTRA_ACTION_WRITE);
        Log.d("cumulus:plugin", "Saving changes");
        sendBroadcast(i);
        finish();
    }

    /**
     * Deletes the provided channel and resyncs. Then the app closes.
     * @param jsonChannel The jsonchannel to delete
     */
    public void deleteChannel(JSONChannel jsonChannel) {
        String jsonString = jsonChannel.toString();
        Intent i = new Intent("com.felkertech.cumulustv.RECEIVER");
        i.putExtra(DataReceiver.INTENT_EXTRA_JSON, jsonString);
        i.putExtra(DataReceiver.INTENT_EXTRA_ACTION, DataReceiver.INTENT_EXTRA_ACTION_DELETE);
        sendBroadcast(i);
        finish();
    }

    /**
     * You made special modifications to the database and you're requesting a sync
     */
    public void saveDatabase() {
        Intent i = new Intent("com.felkertech.cumulustv.RECEIVER");
        i.putExtra(DataReceiver.INTENT_EXTRA_ACTION, DataReceiver.INTENT_EXTRA_ACTION_DATABASE_WRITE);
        sendBroadcast(i);
        finish();
    }

    public JSONChannel getChannel() {
        if(!telegram.hasExtra(INTENT_EXTRA_ACTION))
            return null;
        if(telegram.getStringExtra(INTENT_EXTRA_ACTION).equals(INTENT_ADD))
            return null;
        String number = telegram.getStringExtra(INTENT_EXTRA_NUMBER);
        String name = telegram.getStringExtra(INTENT_EXTRA_NAME);
        String logo = telegram.getStringExtra(INTENT_EXTRA_ICON);
        String url = telegram.getStringExtra(INTENT_EXTRA_URL);
        String splash = telegram.getStringExtra(INTENT_EXTRA_SPLASH);
        String genres = telegram.getStringExtra(INTENT_EXTRA_GENRES);
        return new JSONChannel(number, name, url, logo, splash, genres);
    }

    /**
     * For certain plugins, you may want to lock editing from other apps so you don't disrupt
     * something important. For others, you don't care. This boolean enables or disables that
     * feature
     * @param enabled
     */
    public void setProprietaryEditing(boolean enabled) {
        proprietary = enabled;
    }

    public String getAllChannels() {
        if(telegram.hasExtra(INTENT_EXTRA_ALL_CHANNELS)) {
            return telegram.getStringExtra(INTENT_EXTRA_ALL_CHANNELS);
        } else {
            return "";
        }
    }
}
