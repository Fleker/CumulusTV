package com.felkertech.n.cumulustv;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.media.tv.TvContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.android.sampletvinput.syncadapter.SyncUtils;

import org.json.JSONException;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "cumulus:MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final String info = TvContract.buildInputId(new ComponentName("com.felkertech.n.cumulustv", ".SampleTvInput"));
        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title("Create a new channel")
                        .customView(R.layout.dialog_channel_new, true)
                        .positiveText("Create")
                        .negativeText("Cancel")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                Log.d(TAG, "Submission");
                                //Get stuff
                                LinearLayout l = (LinearLayout) dialog.getCustomView();
                                String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                Log.d(TAG, "Channel " + number);
                                String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                try {
                                    Log.d(TAG, cd.toString());
                                    JSONChannel jsch = new JSONChannel(number, name, stream, logo);
                                    if (cd.channelExists(jsch)) {
                                        Toast.makeText(getApplicationContext(), "Channel already exists", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "no");
                                    } else {
                                        cd.add(jsch);
                                        Log.d(TAG, "K");
                                    }
                                    Log.d(TAG, cd.toString());
                                    SyncUtils.requestSync(info);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .show();

            }
        });
        findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ChannelDatabase cdn = new ChannelDatabase(getApplicationContext());
                String[] channelnames = cdn.getChannelNames();
                if(channelnames.length == 0) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title("You have no streams")
                            .content("Do you want to add one of these? (TO BE FINISHED)")
                            .show();
                } else {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title("My Streams")
                            .items(channelnames)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog materialDialog, View view, final int i, CharSequence charSequence) {
                                    Toast.makeText(getApplicationContext(), charSequence + " selected", Toast.LENGTH_SHORT).show();
                                    final MaterialDialog md = new MaterialDialog.Builder(MainActivity.this)
                                            .title("Edit Stream")
                                            .positiveText("Update")
                                            .negativeText("Delete")
                                            .neutralText("Cancel")
                                            .customView(R.layout.dialog_channel_new, true)
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    super.onPositive(dialog);
                                                    LinearLayout l = (LinearLayout) dialog.getCustomView();
                                                    String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                                    Log.d(TAG, "Channel " + number);
                                                    String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                                    String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                                    String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                                    ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                                    try {
                                                        Log.d(TAG, cd.toString());
                                                        JSONChannel jsch = new JSONChannel(number, name, stream, logo);
                                                        cd.update(jsch);
                                                        Log.d(TAG, cd.toString());
                                                        SyncUtils.requestSync(info);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }

                                                @Override
                                                public void onNegative(MaterialDialog dialog) {
                                                    super.onNegative(dialog);
                                                    final LinearLayout l = (LinearLayout) dialog.getCustomView();
                                                    String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                                    Log.d(TAG, "Channel " + number);
                                                    String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                                    String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                                    String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                                    new MaterialDialog.Builder(MainActivity.this)
                                                            .title("Delete?")
                                                            .positiveText("Yes")
                                                            .negativeText("No")
                                                            .callback(new MaterialDialog.ButtonCallback() {
                                                                @Override
                                                                public void onPositive(MaterialDialog dialog) {
                                                                    super.onPositive(dialog);
                                                                    String number = ((EditText) l.findViewById(R.id.number)).getText().toString();
                                                                    Log.d(TAG, "DEL Channel " + number);
                                                                    String name = ((EditText) l.findViewById(R.id.name)).getText().toString();
                                                                    String logo = ((EditText) l.findViewById(R.id.logo)).getText().toString();
                                                                    String stream = ((EditText) l.findViewById(R.id.stream)).getText().toString();
                                                                    ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                                                    try {
                                                                        Log.d(TAG, cd.toString());
                                                                        JSONChannel jsch = new JSONChannel(number, name, stream, logo);
                                                                        cd.delete(jsch);
                                                                        Log.d(TAG, cd.toString());
                                                                        SyncUtils.requestSync(info);
                                                                    } catch (JSONException e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                }
                                                            }).show();
                                                }
                                            })
                                            .show();
                                    md.setOnShowListener(new DialogInterface.OnShowListener() {
                                        @Override
                                        public void onShow(DialogInterface dialog) {
                                            try {
                                                JSONChannel jsonChannel = new JSONChannel(cdn.getJSONChannels().getJSONObject(i));
                                                LinearLayout l = (LinearLayout) md.getCustomView();
                                                ((EditText) l.findViewById(R.id.number)).setText(jsonChannel.getNumber());
                                                Log.d(TAG, "Channel " + jsonChannel.getNumber());
                                                ((EditText) l.findViewById(R.id.name)).setText(jsonChannel.getName());
                                                ((EditText) l.findViewById(R.id.logo)).setText(jsonChannel.getLogo());
                                                ((EditText) l.findViewById(R.id.stream)).setText(jsonChannel.getUrl());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            })
                            .show();
                }
            }
        });
        findViewById(R.id.suggested).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                0xx NEWS
                1xx SCIENCE/TECH/NATURE
                2xx HISTORY/EDUCATION
                3xx SPORTS/VIDEO GAMES
                4xx MUSIC
                5xx FICTION
                6xx NONFICTION
                7xx GOVERNMENT/SOCIETY
                9xx MISC
                 */
                final JSONChannel[] channels = { /* Some via http://rgw.ustream.tv/json.php/Ustream.searchBroadcast/ */
                        new JSONChannel("900", "Artbeats Demo", "http://cdn-fms.rbs.com.br/hls-vod/sample1_1500kbps.f4v.m3u8", ""),
                        new JSONChannel("100", "NASA Public", "http://iphone-streaming.ustream.tv/uhls/6540154/streams/live/iphone/playlist.m3u8", "http://static-cdn1.ustream.tv/i/channel/live/1_6540154,256x144,b:2015071514.jpg"),
                        new JSONChannel("167", "Montery Bay Aquarium", "http://iphone-streaming.ustream.tv/uhls/9600798/streams/live/iphone/playlist.m3u8", "http://static-cdn1.ustream.tv/i/channel/live/1_9600798,256x144,b:2015071514.jpg"),
                        new JSONChannel("168", "Audubon Osprey Cam", "http://iphone-streaming.ustream.tv/uhls/11378037/streams/live/iphone/playlist.m3u8", "http://static-cdn1.ustream.tv/i/channel/live/1_11378037,256x144,b:2015071514.jpg"),
                        new JSONChannel("101", "ISS Stream", "http://iphone-streaming.ustream.tv/uhls/9408562/streams/live/iphone/playlist.m3u8", "http://static-cdn1.ustream.tv/i/channel/picture/9/4/0/8/9408562/9408562_iss_hr_1330361780,256x144,r:1.jpg"),
                        new JSONChannel("400", "Beats One", "http://stream.connectcast.tv:1935/live/CC-EC1245DB-5C6A-CF57-D13A-BB36B3CBB488-34313/playlist.m3u8", "")
                };
                ArrayList<String> channeltext = new ArrayList<String>();
                for(JSONChannel j: channels) {
                    channeltext.add(j.getName());
                }
                final String[] channelList = channeltext.toArray(new String[channeltext.size()]);
                new MaterialDialog.Builder(MainActivity.this)
                        .title("Here are some suggested streams:")
                        .items(channelList)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                JSONChannel j = channels[i];
                                ChannelDatabase cd = new ChannelDatabase(getApplicationContext());
                                if(cd.channelExists(j)) {
                                    Toast.makeText(getApplicationContext(), "Channel already added", Toast.LENGTH_SHORT).show();
                                } else {
                                    try {
                                        Toast.makeText(getApplicationContext(), charSequence+" has been added", Toast.LENGTH_SHORT).show();
                                        cd.add(j);
                                        SyncUtils.requestSync(info);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).show();
            }
        });
    }
}
